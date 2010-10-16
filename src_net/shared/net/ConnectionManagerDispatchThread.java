/**
 * <p>
 * Copyright (c) 2009 Roy Liu<br>
 * All rights reserved.
 * </p>
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * </p>
 * <ul>
 * <li>Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.</li>
 * <li>Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.</li>
 * <li>Neither the name of the author nor the names of any contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.</li>
 * </ul>
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * </p>
 */

package shared.net;

import static shared.net.InterestEvent.InterestEventType.DISPATCH;
import static shared.net.InterestEvent.InterestEventType.GET_CONNECTIONS;
import static shared.net.InterestEvent.InterestEventType.SHUTDOWN;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import shared.event.Handler;
import shared.event.Transitions;
import shared.event.Transitions.Transition;
import shared.net.AbstractManagedConnection.AbstractManagedConnectionStatus;
import shared.net.AcceptRegistry.Entry;
import shared.util.Control;
import shared.util.RequestFuture;

/**
 * A specialized {@link ConnectionManagerThread} that dispatches newly created connections to
 * {@link ConnectionManagerIoThread}s.
 * 
 * @apiviz.composedOf shared.net.ConnectionManagerIoThread
 * @apiviz.composedOf shared.net.AcceptRegistry
 * @author Roy Liu
 */
public class ConnectionManagerDispatchThread extends ConnectionManagerThread {

    @Override
    protected void onStart() {
        initFsms();
    }

    @Override
    protected void onStop() {

        // Tell the bad news to all pending connections.
        for (SelectionKey key : this.selector.keys()) {

            Object attachment = key.attachment();

            if (attachment instanceof Entry) {

                Set<AbstractManagedConnection<?>> pending = ((Entry) attachment).getPending();

                // Copy the collection to prevent concurrent modification.
                for (AbstractManagedConnection<?> pendingConn : new ArrayList<AbstractManagedConnection<?>>(pending)) {
                    handleError(pendingConn, this.exception);
                }

                // All pending connections must have been deregistered.
                assert pending.isEmpty();
            }
        }

        for (ConnectionManagerIoThread ioThread : this.ioThreads) {
            ioThread.onLocal(new InterestEvent<Object>(SHUTDOWN, null));
        }
    }

    @Override
    protected void doReadyOps(int readyOps, SelectionKey key) {

        // Each operation is responsible for its own exception handling.

        if ((readyOps & SelectionKey.OP_ACCEPT) != 0) {
            doAccept(key);
        }

        if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
            doConnect((AbstractManagedConnection<?>) key.attachment());
        }
    }

    @Override
    protected void purge(AbstractManagedConnection<?> conn) {
        this.acceptRegistry.removePending(conn);
    }

    /**
     * Starts this thread and its helper {@link ConnectionManagerIoThread}s.
     */
    @Override
    public void start() {

        for (ConnectionManagerThread ioThread : this.ioThreads) {
            ioThread.start();
        }

        super.start();
    }

    /**
     * Dispatches the given connection to a {@link ConnectionManagerIoThread}.
     */
    protected void dispatch(AbstractManagedConnection<?> conn) {

        ConnectionManagerIoThread ioThread = this.ioThreads.removeFirst();
        this.ioThreads.add(ioThread);

        // Break the connection's relationship with this thread.
        conn.deregisterKey();

        // Acquire the connection monitor to shut out external requests.
        synchronized (conn.getLock()) {

            conn.setThread(ioThread);
            ioThread.onLocal(new InterestEvent<Object>(DISPATCH, conn.getProxy()));
        }
    }

    /**
     * Finishes the accept cycle on a ready {@link ServerSocketChannel}.
     */
    protected void doAccept(SelectionKey key) {

        Entry entry = (Entry) key.attachment();
        ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();

        Set<AbstractManagedConnection<?>> pending = entry.getPending();

        // We had better have pending accepts.
        assert !pending.isEmpty();

        AbstractManagedConnection<?> conn = pending.iterator().next();

        // The connection had better be in the correct state.
        assert (conn.getStatus() == AbstractManagedConnectionStatus.ACCEPT);

        try {

            final SocketChannel channel;

            try {

                channel = ssChannel.accept();

            } catch (IOException e) {

                this.acceptRegistry.removePending(conn);

                // Copy the collection to prevent concurrent modification.
                for (AbstractManagedConnection<?> pendingConn : new ArrayList<AbstractManagedConnection<?>>(pending)) {
                    handleError(pendingConn, e);
                }

                // All pending connections must have been deregistered.
                assert pending.isEmpty();

                throw e;
            }

            this.acceptRegistry.removePending(conn);

            conn.setup(channel);
            conn.doBind();

            debug("[%s] accepted at \"%s\".", conn, conn.getLocalAddress());

            conn.setStatus(AbstractManagedConnectionStatus.ACTIVE);

            dispatch(conn);

        } catch (Throwable t) {

            handleError(conn, t);
        }
    }

    /**
     * Finishes the connect cycle on a ready connection.
     */
    protected void doConnect(AbstractManagedConnection<?> conn) {

        // The connection had better be in the correct state.
        assert (conn.getStatus() == AbstractManagedConnectionStatus.CONNECT);

        try {

            // It had better be the case that this method either throws an exception or returns true.
            Control.checkTrue(((SocketChannel) conn.getKey().channel()).finishConnect(), //
                    "Expected to finish connecting");

            conn.doBind();

            debug("[%s] connected to \"%s\".", conn, conn.getRemoteAddress());

            conn.setStatus(AbstractManagedConnectionStatus.ACTIVE);

            dispatch(conn);

        } catch (Throwable t) {

            handleError(conn, t);
        }
    }

    /**
     * Handles a connection connect request.
     */
    protected void handleConnect(AbstractManagedConnection<?> conn, InetSocketAddress address) {

        // The connection had better be in the correct state.
        assert (conn.getStatus() == AbstractManagedConnectionStatus.VIRGIN);

        final boolean connectImmediately;

        try {

            SocketChannel channel = SocketChannel.open();

            conn.setup(channel);
            conn.registerKey(this.selector, SelectionKey.OP_CONNECT);

            connectImmediately = channel.connect(address);

            debug("[%s] connect to \"%s\".", conn, address);

            conn.setStatus(AbstractManagedConnectionStatus.CONNECT);

        } catch (Throwable t) {

            handleError(conn, t);

            return;
        }

        // If connected for some reason, then finish up the process.
        if (connectImmediately) {
            doConnect(conn);
        }
    }

    /**
     * Handles a connection accept request.
     */
    protected void handleAccept(AbstractManagedConnection<?> conn, InetSocketAddress address) {

        // The connection had better be in the correct state.
        assert (conn.getStatus() == AbstractManagedConnectionStatus.VIRGIN);

        try {

            Entry entry = this.acceptRegistry.register(conn, address);
            entry.getKey().attach(entry);

            debug("[%s] listen at \"%s\" (%d in queue).", //
                    conn, entry.getAddress(), entry.getPending().size());

            conn.setStatus(AbstractManagedConnectionStatus.ACCEPT);

        } catch (Throwable t) {

            handleError(conn, t);
        }
    }

    /**
     * Handles a connection registration request.
     */
    protected void handleRegister(AbstractManagedConnection<?> conn, SocketChannel chan) {

        // The connection had better be in the correct state.
        assert (conn.getStatus() == AbstractManagedConnectionStatus.VIRGIN);

        try {

            // Set up the connection and simulate deferred writes.
            conn.setup(chan);
            conn.doBind();

            debug("[%s] registered.", conn);

            conn.setStatus(AbstractManagedConnectionStatus.ACTIVE);

            dispatch(conn);

        } catch (Throwable t) {

            handleError(conn, t);
        }
    }

    /**
     * Handles a request to get the list of bound addresses.
     */
    protected void handleGetBoundAddresses(RequestFuture<List<InetSocketAddress>> future) {
        future.set(new ArrayList<InetSocketAddress>(this.acceptRegistry.getAddresses()));
    }

    /**
     * Handles a request to get the list of connections, which is an aggregate of the lists reported by helper
     * {@link ConnectionManagerIoThread}s.
     */
    @SuppressWarnings("unchecked")
    protected void handleGetConnections(RequestFuture<List<AbstractManagedConnection<?>>> future) {

        List<AbstractManagedConnection<?>> res = new ArrayList<AbstractManagedConnection<?>>();

        for (ConnectionManagerIoThread ioThread : this.ioThreads) {
            res.addAll((List<AbstractManagedConnection<?>>) ioThread.request(GET_CONNECTIONS));
        }

        future.set(res);
    }

    @Transition(currentState = "VIRGIN", eventType = "CONNECT")
    final Handler<InterestEvent<InetSocketAddress>> connectHandler = new Handler<InterestEvent<InetSocketAddress>>() {

        @Override
        public void handle(InterestEvent<InetSocketAddress> evt) {
            handleConnect(((ProxySource<?>) evt.getSource()).getConnection(), evt.getArgument());
        }
    };

    @Transition(currentState = "VIRGIN", eventType = "ACCEPT")
    final Handler<InterestEvent<InetSocketAddress>> acceptHandler = new Handler<InterestEvent<InetSocketAddress>>() {

        @Override
        public void handle(InterestEvent<InetSocketAddress> evt) {
            handleAccept(((ProxySource<?>) evt.getSource()).getConnection(), evt.getArgument());
        }
    };

    @Transition(currentState = "VIRGIN", eventType = "REGISTER")
    final Handler<InterestEvent<SocketChannel>> registerHandler = new Handler<InterestEvent<SocketChannel>>() {

        @Override
        public void handle(InterestEvent<SocketChannel> evt) {
            handleRegister(((ProxySource<?>) evt.getSource()).getConnection(), evt.getArgument());
        }
    };

    @Transitions(transitions = {
            //
            @Transition(currentState = "CONNECT", eventType = "CLOSE"), //
            @Transition(currentState = "ACCEPT", eventType = "CLOSE") //
    })
    final Handler<InterestEvent<?>> closeHandler = new Handler<InterestEvent<?>>() {

        @Override
        public void handle(InterestEvent<?> evt) {
            handleClose(((ProxySource<?>) evt.getSource()).getConnection());
        }
    };

    @Transitions(transitions = {
            //
            @Transition(currentState = "VIRGIN", eventType = "ERROR"), //
            @Transition(currentState = "CONNECT", eventType = "ERROR"), //
            @Transition(currentState = "ACCEPT", eventType = "ERROR") //
    })
    final Handler<InterestEvent<Throwable>> errorHandler = new Handler<InterestEvent<Throwable>>() {

        @Override
        public void handle(InterestEvent<Throwable> evt) {
            handleError(((ProxySource<?>) evt.getSource()).getConnection(), evt.getArgument());
        }
    };

    @Transitions(transitions = {
            //
            @Transition(currentState = "CONNECT", eventType = "EXECUTE"), //
            @Transition(currentState = "ACCEPT", eventType = "EXECUTE") //
    })
    final Handler<InterestEvent<Runnable>> executeHandler = new Handler<InterestEvent<Runnable>>() {

        @Override
        public void handle(InterestEvent<Runnable> evt) {
            handleExecute(((ProxySource<?>) evt.getSource()).getConnection(), evt.getArgument());
        }
    };

    @Transition(currentState = "RUN", eventType = "GET_BOUND_ADDRESSES", group = "internal")
    final Handler<InterestEvent<RequestFuture<List<InetSocketAddress>>>> getBoundAddressesHandler = //
    new Handler<InterestEvent<RequestFuture<List<InetSocketAddress>>>>() {

        @Override
        public void handle(InterestEvent<RequestFuture<List<InetSocketAddress>>> evt) {
            handleGetBoundAddresses(evt.getArgument());
        }
    };

    @Transition(currentState = "RUN", eventType = "GET_CONNECTIONS", group = "internal")
    final Handler<InterestEvent<RequestFuture<List<AbstractManagedConnection<?>>>>> getConnectionsHandler = //
    new Handler<InterestEvent<RequestFuture<List<AbstractManagedConnection<?>>>>>() {

        @Override
        public void handle(InterestEvent<RequestFuture<List<AbstractManagedConnection<?>>>> evt) {
            handleGetConnections(evt.getArgument());
        }
    };

    @Transition(currentState = "RUN", eventType = "SHUTDOWN", group = "internal")
    final Handler<InterestEvent<?>> shutdownHandler = new Handler<InterestEvent<?>>() {

        @Override
        public void handle(InterestEvent<?> evt) {
            handleShutdown();
        }
    };

    final AcceptRegistry acceptRegistry;
    final LinkedList<ConnectionManagerIoThread> ioThreads;
    final int backlogSize;

    /**
     * Default constructor.
     */
    protected ConnectionManagerDispatchThread(String name, int backlogSize, int nThreads) {
        super(String.format("%s/Dispatch", name));

        this.acceptRegistry = new AcceptRegistry(this.selector, this.backlogSize);

        this.ioThreads = new LinkedList<ConnectionManagerIoThread>();

        for (int i = 0; i < nThreads; i++) {
            this.ioThreads.add(new ConnectionManagerIoThread(String.format("%s/IO-%d", name, i), this));
        }

        this.backlogSize = backlogSize;
    }
}
