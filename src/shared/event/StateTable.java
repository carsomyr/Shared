/**
 * This file is part of the Shared Scientific Toolbox in Java ("this library"). <br />
 * <br />
 * Copyright (C) 2005 Roy Liu <br />
 * <br />
 * This library is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 2.1 of the License, or (at your option)
 * any later version. <br />
 * <br />
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details. <br />
 * <br />
 * You should have received a copy of the GNU Lesser General Public License along with this library. If not, see <a
 * href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
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
 * @apiviz.has shared.event.Transitions - - - argument
 * @apiviz.has shared.event.Event - - - event
 * @param <X>
 *            the state enumeration type.
 * @param <Y>
 *            the {@link Event} enumeration type.
 * @param <Z>
 *            the parameterization lower bounded by {@link Event} itself.
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
    @SuppressWarnings("unchecked")
    public StateTable(Object target, Class<X> stateClass, Class<Y> eventTypeClass, String group) {

        this.backingArray = new ObjectArray( //
                StateHandler.class, //
                stateClass.getEnumConstants().length, //
                eventTypeClass.getEnumConstants().length);

        final Map<String, List<StateHandler>> handlersMap = //
        new HashMap<String, List<StateHandler>>();

        for (String str : WildcardCombinations) {
            handlersMap.put(str, new ArrayList<StateHandler>());
        }

        for (Class<?> clazz = target.getClass(); clazz != null //
                && !clazz.getName().startsWith("java.") //
                && !clazz.getName().startsWith("javax."); clazz = clazz.getSuperclass()) {

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

                Control.checkTrue(obj instanceof Handler, //
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
                        (stateHandler.state != null) ? new int[] { stateHandler.state //
                                .ordinal() } : rowRange, //
                        (stateHandler.eventType != null) ? new int[] { stateHandler.eventType //
                                .ordinal() } : colRange //
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

        int nrows = this.backingArray.size(0);
        int ncols = this.backingArray.size(1);

        for (int i = 0; i < nrows; i++) {

            for (int j = 0; j < ncols; j++) {

                StateHandler stateHandler = this.backingArray.get(i, j);

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
