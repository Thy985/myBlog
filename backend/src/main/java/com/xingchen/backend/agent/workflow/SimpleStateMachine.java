package com.xingchen.backend.agent.workflow;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Data
@Slf4j
public class SimpleStateMachine<S, E> {

    private final Map<S, Map<E, Transition<S>>> transitions = new ConcurrentHashMap<>();
    private final Map<S, Consumer<TransitionContext<S>>> stateEntryActions = new ConcurrentHashMap<>();
    private final Map<S, Consumer<TransitionContext<S>>> stateExitActions = new ConcurrentHashMap<>();

    private S currentState;
    private final String machineId;

    public SimpleStateMachine() {
        this.machineId = UUID.randomUUID().toString();
    }

    public SimpleStateMachine(S initialState) {
        this.machineId = UUID.randomUUID().toString();
        this.currentState = initialState;
    }

    public SimpleStateMachine<S, E> addTransition(S from, E event, S to) {
        return addTransition(from, event, to, () -> {});
    }

    public SimpleStateMachine<S, E> addTransition(S from, E event, S to, Runnable action) {
        transitions.computeIfAbsent(from, k -> new ConcurrentHashMap<>())
                .put(event, new Transition<>(from, event, to, action));
        return this;
    }

    public SimpleStateMachine<S, E> addTransition(S from, E event, S to, Consumer<TransitionContext<S>> action) {
        transitions.computeIfAbsent(from, k -> new ConcurrentHashMap<>())
                .put(event, new Transition<>(from, event, to, action != null ? () -> action.accept(null) : () -> {}));
        return this;
    }

    public TransitionResult<S> transition(E event) {
        return transition(event, null);
    }

    public TransitionResult<S> transition(E event, Map<String, Object> contextData) {
        if (currentState == null) {
            return TransitionResult.error("State machine not initialized");
        }

        Transition<S> transition = getTransition(currentState, event);
        if (transition == null) {
            return TransitionResult.error("Invalid transition: " + currentState + " + " + event);
        }

        S fromState = currentState;
        long startTime = System.currentTimeMillis();

        try {
            executeExitActions(fromState, contextData);

            currentState = transition.getTargetState();

            executeEntryActions(currentState, contextData);

            transition.getAction().run();

            long duration = System.currentTimeMillis() - startTime;
            log.debug("[{}] State transition: {} + {} -> {} ({}ms)",
                    machineId, fromState, event, currentState, duration);

            return TransitionResult.success(fromState, event, currentState);

        } catch (Exception e) {
            log.error("[{}] Transition error: {} + {} -> {}", machineId, fromState, event, e.getMessage());
            return TransitionResult.error("Transition failed: " + e.getMessage());
        }
    }

    public S getCurrentState() {
        return currentState;
    }

    public void setState(S state) {
        this.currentState = state;
    }

    public boolean canTransition(E event) {
        return getTransition(currentState, event) != null;
    }

    public Set<E> getAvailableEvents() {
        if (currentState == null) return Collections.emptySet();
        Map<E, Transition<S>> stateTransitions = transitions.get(currentState);
        return stateTransitions != null ? stateTransitions.keySet() : Collections.emptySet();
    }

    public Transition<S> getTransition(S state, E event) {
        if (state == null) return null;
        Map<E, Transition<S>> stateTransitions = transitions.get(state);
        return stateTransitions != null ? stateTransitions.get(event) : null;
    }

    public SimpleStateMachine<S, E> onStateEntry(S state, Consumer<TransitionContext<S>> action) {
        stateEntryActions.put(state, action);
        return this;
    }

    public SimpleStateMachine<S, E> onStateExit(S state, Consumer<TransitionContext<S>> action) {
        stateExitActions.put(state, action);
        return this;
    }

    private void executeEntryActions(S state, Map<String, Object> contextData) {
        Consumer<TransitionContext<S>> action = stateEntryActions.get(state);
        if (action != null) {
            try {
                TransitionContext<S> ctx = new TransitionContext<>(currentState, null, contextData);
                action.accept(ctx);
            } catch (Exception e) {
                log.warn("[{}] State entry action error: {}", machineId, e.getMessage());
            }
        }
    }

    private void executeExitActions(S state, Map<String, Object> contextData) {
        Consumer<TransitionContext<S>> action = stateExitActions.get(state);
        if (action != null) {
            try {
                TransitionContext<S> ctx = new TransitionContext<>(state, null, contextData);
                action.accept(ctx);
            } catch (Exception e) {
                log.warn("[{}] State exit action error: {}", machineId, e.getMessage());
            }
        }
    }

    public Map<String, Object> getSnapshot() {
        return Map.of(
                "machineId", machineId,
                "currentState", currentState != null ? currentState.toString() : "null",
                "availableEvents", getAvailableEvents()
        );
    }

    @Data
    public static class Transition<S> {
        private final S fromState;
        private final Object event;
        private final S targetState;
        private final Runnable action;

        public Transition(S fromState, Object event, S targetState, Runnable action) {
            this.fromState = fromState;
            this.event = event;
            this.targetState = targetState;
            this.action = action != null ? action : () -> {};
        }
    }

    @Data
    public static class TransitionContext<S> {
        private final S fromState;
        private final Object event;
        private final Map<String, Object> data;

        public TransitionContext(S fromState, Object event, Map<String, Object> data) {
            this.fromState = fromState;
            this.event = event;
            this.data = data != null ? data : Map.of();
        }
    }

    @Data
    public static class TransitionResult<S> {
        private final boolean success;
        private final S fromState;
        private final Object event;
        private final S toState;
        private final String error;

        private TransitionResult(boolean success, S fromState, Object event, S toState, String error) {
            this.success = success;
            this.fromState = fromState;
            this.event = event;
            this.toState = toState;
            this.error = error;
        }

        public static <S> TransitionResult<S> success(S fromState, Object event, S toState) {
            return new TransitionResult<>(true, fromState, event, toState, null);
        }

        public static <S> TransitionResult<S> error(String error) {
            return new TransitionResult<>(false, null, null, null, error);
        }
    }
}
