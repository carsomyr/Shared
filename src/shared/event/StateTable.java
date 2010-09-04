/**
 * <p>
 * Copyright (c) 2005 Roy Liu<br>
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

package shared.event;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import shared.array.ObjectArray;
import shared.event.Transitions.Transition;
import shared.util.Arithmetic;
import shared.util.Control;

/**
 * A finite state machine class.
 * 
 * @apiviz.composedOf shared.event.StateTable.StateHandler
 * @apiviz.has shared.event.Event - - - event
 * @apiviz.has shared.event.Transitions - - - argument
 * @param <X>
 *            the state enumeration type.
 * @param <Y>
 *            the {@link Event} enumeration type.
 * @param <Z>
 *            the {@link Event} type.
 * @author Roy Liu
 */
public class StateTable<X extends Enum<X>, Y extends Enum<Y>, Z extends Event<Z, Y, ?>> {

    /**
     * An array of all four wildcard combinations.
     */
    final protected static String[] WildcardCombinations = new String[] { "**", "* ", " *", "  " };

    final ObjectArray<StateHandler> backingArray;

    /**
     * Default constructor.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public StateTable(Object target, Class<X> stateClass, Class<Y> eventTypeClass, String group) {

        this.backingArray = new ObjectArray( //
                StateHandler.class, //
                stateClass.getEnumConstants().length, //
                eventTypeClass.getEnumConstants().length);

        final Map<String, List<StateHandler>> handlersMap = new HashMap<String, List<StateHandler>>();

        for (String str : WildcardCombinations) {
            handlersMap.put(str, new ArrayList<StateHandler>());
        }

        for (Class<?> clazz = target.getClass(); //
        clazz != null && !clazz.getName().startsWith("java.") && !clazz.getName().startsWith("javax."); //
        clazz = clazz.getSuperclass()) {

            outerLoop: for (Field field : clazz.getDeclaredFields()) {

                Transitions ts = field.getAnnotation(Transitions.class);
                Transition t = field.getAnnotation(Transition.class);

                Control.checkTrue(ts == null || t == null, //
                        "Transition and Transitions annotations cannot occur simultaneously");

                final Transition[] transitions;

                if (ts != null) {

                    transitions = ts.transitions();

                } else if (t != null) {

                    transitions = new Transition[] { t };

                } else {

                    continue outerLoop;
                }

                final Object obj;

                field.setAccessible(true);

                try {

                    obj = field.get(target);

                } catch (IllegalAccessException e) {

                    throw new RuntimeException(e);

                } finally {

                    field.setAccessible(false);
                }

                Control.checkTrue(obj instanceof Handler<?>, //
                        "Field does not reference an event handler");

                final Handler<Z> handler = (Handler<Z>) obj;
                final String name = field.getName();

                innerLoop: for (Transition transition : transitions) {

                    if (!transition.group().equals(group)) {
                        continue innerLoop;
                    }

                    X currentState = !transition.currentState().equals("*") ? Enum.valueOf( //
                            stateClass, transition.currentState()) : null;
                    Y eventType = !transition.eventType().equals("*") ? Enum.valueOf( //
                            eventTypeClass, transition.eventType()) : null;

                    final StateHandler stateHandler;

                    if (!transition.nextState().equals("")) {

                        final X nextState = Enum.valueOf(stateClass, transition.nextState());

                        stateHandler = new StateHandler(currentState, eventType) {

                            @Override
                            public void handle(EnumStatus<X> stateObj, Z evt) {

                                handler.handle(evt);
                                stateObj.setStatus(nextState);
                            }

                            @Override
                            public String toString() {
                                return String.format("%s -> %s : %s", //
                                        super.toString(), nextState, name);
                            }
                        };

                    } else {

                        stateHandler = new StateHandler(currentState, eventType) {

                            @Override
                            public void handle(EnumStatus<X> stateObj, Z evt) {
                                handler.handle(evt);
                            }

                            @Override
                            public String toString() {
                                return String.format("%s : %s", //
                                        super.toString(), name);
                            }
                        };
                    }

                    final String key;

                    if (currentState == null && eventType == null) {

                        key = "**";

                    } else if (currentState == null && eventType != null) {

                        key = "* ";

                    } else if (currentState != null && eventType == null) {

                        key = " *";

                    } else {

                        key = "  ";
                    }

                    handlersMap.get(key).add(stateHandler);
                }
            }
        }

        int[] rowRange = Arithmetic.range(this.backingArray.size(0));
        int[] colRange = Arithmetic.range(this.backingArray.size(1));

        for (String key : WildcardCombinations) {

            for (StateHandler stateHandler : handlersMap.get(key)) {

                int[][] slices = new int[][] {
                        //
                        (stateHandler.state != null) ? new int[] { stateHandler.state.ordinal() } : rowRange, //
                        (stateHandler.eventType != null) ? new int[] { stateHandler.eventType.ordinal() } : colRange //
                };

                this.backingArray.slice(stateHandler, slices);
            }
        }
    }

    /**
     * Alternate constructor.
     */
    public StateTable(Object target, Class<X> stateClass, Class<Y> eventTypeClass) {
        this(target, stateClass, eventTypeClass, "");
    }

    /**
     * Creates a human-readable representation of this table.
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        int nRows = this.backingArray.size(0);
        int nCols = this.backingArray.size(1);

        for (int row = 0; row < nRows; row++) {

            for (int col = 0; col < nCols; col++) {

                StateHandler stateHandler = this.backingArray.get(row, col);

                if (stateHandler != null) {
                    sb.append(stateHandler).append(Control.LineSeparator);
                }
            }
        }

        return sb.toString();
    }

    /**
     * Looks up and handles an {@link Event} based on the current state and the event type.
     * 
     * @param stateObj
     *            the {@link EnumStatus} object.
     * @param evt
     *            the {@link Event}.
     */
    public void lookup(EnumStatus<X> stateObj, Z evt) {

        StateHandler handler = this.backingArray.get(stateObj.getStatus().ordinal(), evt.getType().ordinal());

        if (handler != null) {
            handler.handle(stateObj, evt);
        }
    }

    /**
     * Defines an {@link Event} handler that may mutate {@link EnumStatus} objects.
     */
    abstract protected class StateHandler {

        /**
         * The state.
         */
        final protected X state;

        /**
         * The event type.
         */
        final protected Y eventType;

        /**
         * Default constructor.
         */
        protected StateHandler(X state, Y eventType) {

            this.state = state;
            this.eventType = eventType;
        }

        /**
         * Creates a human-readable representation of this handler.
         */
        @Override
        public String toString() {
            return String.format("(%s, %s)", //
                    (this.state != null) ? this.state : "*", //
                    (this.eventType != null) ? this.eventType : "*");
        }

        /**
         * Handles an {@link Event}. May optionally mutate the given {@link EnumStatus} object.
         */
        abstract protected void handle(EnumStatus<X> state, Z evt);
    }
}
