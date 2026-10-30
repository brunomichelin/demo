package com.example.demo.config;

import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.event.PreDestroyViewMapEvent;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.event.SystemEventListener;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import java.util.HashMap;
import java.util.Map;

public class FacesViewScope implements Scope {

    private static final String CALLBACKS_KEY = FacesViewScope.class.getName() + ".callbacks";
    private static final String LISTENER_REGISTERED_KEY = FacesViewScope.class.getName() + ".listener";

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        UIViewRoot viewRoot = getViewRoot();
        Map<String, Object> viewMap = viewRoot.getViewMap(true);
        registerPreDestroyListener(viewRoot, viewMap);
        return viewMap.computeIfAbsent(name, key -> objectFactory.getObject());
    }

    @Override
    public Object remove(String name) {
        Map<String, Object> viewMap = getViewRoot().getViewMap(true);
        Map<String, Runnable> callbacks = getCallbacks(viewMap);
        Runnable callback = callbacks.remove(name);
        if (callback != null) {
            callback.run();
        }
        return viewMap.remove(name);
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        UIViewRoot viewRoot = getViewRoot();
        Map<String, Object> viewMap = viewRoot.getViewMap(true);
        registerPreDestroyListener(viewRoot, viewMap);
        getCallbacks(viewMap).put(name, callback);
    }

    @Override
    public Object resolveContextualObject(String key) {
        return null;
    }

    @Override
    public String getConversationId() {
        return getViewRoot().getViewId();
    }

    private static UIViewRoot getViewRoot() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null || facesContext.getViewRoot() == null) {
            throw new IllegalStateException("The Faces view scope is only available during a JSF request.");
        }
        return facesContext.getViewRoot();
    }

    private static void registerPreDestroyListener(UIViewRoot viewRoot, Map<String, Object> viewMap) {
        if (Boolean.TRUE.equals(viewMap.get(LISTENER_REGISTERED_KEY))) {
            return;
        }
        viewMap.put(LISTENER_REGISTERED_KEY, true);
        viewRoot.subscribeToViewEvent(PreDestroyViewMapEvent.class, new SystemEventListener() {
            @Override
            public void processEvent(SystemEvent event) {
                if (event instanceof ComponentSystemEvent componentEvent) {
                    destroyViewBeans(componentEvent);
                }
            }

            @Override
            public boolean isListenerForSource(Object source) {
                return source instanceof UIViewRoot;
            }
        });
    }

    private static void destroyViewBeans(ComponentSystemEvent event) {
        Map<String, Object> viewMap = ((UIViewRoot) event.getComponent()).getViewMap(false);
        if (viewMap == null) {
            return;
        }
        Map<String, Runnable> callbacks = getCallbacks(viewMap);
        callbacks.values().forEach(Runnable::run);
        callbacks.clear();
    }

    private static Map<String, Runnable> getCallbacks(Map<String, Object> viewMap) {
        Object value = viewMap.get(CALLBACKS_KEY);
        if (value instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Runnable> callbacks = (Map<String, Runnable>) value;
            return callbacks;
        }
        Map<String, Runnable> callbacks = new HashMap<>();
        viewMap.put(CALLBACKS_KEY, callbacks);
        return callbacks;
    }
}
