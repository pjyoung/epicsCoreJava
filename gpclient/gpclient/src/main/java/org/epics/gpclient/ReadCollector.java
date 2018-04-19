/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.epics.gpclient;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A collector can be written from one thread and read from another and provides
 * the point where two subsystems and their rate can be decoupled.
 *
 * @param <I> the type written in the collector
 * @param <O> the type read from the collector
 * @author carcassi
 */
public abstract class ReadCollector<I, O> {
    
    class CollectorSupplier implements Supplier<O> {

        @Override
        public O get() {
            return getValue();
        }
        
    }
    
    protected final Object lock = new Object();
    protected Consumer<PVEvent> collectorListener;
    protected boolean connection = false;
    private final Class<I> type;
    private final Supplier<O> readFunction = new CollectorSupplier();

    /**
     * Creates a new read collector of the given type.
     * 
     * @param type the type of values collected; can't be null
     */
    ReadCollector(Class<I> type) {
        if (type == null) {
            throw new NullPointerException("Type can't be null");
        }
        this.type = type;
    }
    
    /**
     * Sets the listener for the events generated by the channel source. Apart
     * from unit tests, the {@link PVDirector} is the one that receives the
     * events. Changing it to null effectively deregisteres the listener.
     * 
     * @param collectorListener the new listeners; can be null
     */
    void setUpdateListener(Consumer<PVEvent> notification) {
        synchronized (lock) {
            this.collectorListener = notification;
        }
    }
    
    /**
     * The read function for the values collected.
     * 
     * @return the read function; can't be null
     */
    Supplier<O> getReadFunction() {
        return readFunction;
    }
    
    /**
     * Returns the collected value/values.
     * 
     * @return the new value
     */
    abstract O getValue();
    
    /**
     * The current connection state.
     * 
     * @return the connection state
     */
    boolean getConnection() {
        synchronized(lock) {
            return connection;
        }
    }
    
    /**
     * The type of values collected.
     * 
     * @return the type of values collected; can't be null
     */
    public Class<I> getType() {
        return type;
    }

    /**
     * Update the value. The channel source implementation can use this method
     * to tell the gpclient that a new is available for processing.
     * 
     * @param value the new value; can be null
     */    
    public abstract void updateValue(I value);

    /**
     * Update the connection state and value. The channel source implementation
     * can use this method to tell the gpclient that both connection and value
     * have changed result in a single event.
     * 
     * @param value the new value; can be null
     * @param newConnection the new connection state
     */
    public abstract void updateValueAndConnection(I value, boolean newConnection);

    /**
     * Update the connection state. The channel source implementation can use
     * this method to tell the gpclient that the ability to write to the channel
     * has changed.
     * 
     * @param newConnection the new connection state
     */
    public void updateConnection(boolean newConnection) {
        Consumer<PVEvent> listener;
        synchronized (lock) {
            connection = newConnection;
            listener = collectorListener;
        }
        // Run the task without holding the lock
        if (listener != null) {
            listener.accept(PVEvent.readConnectionEvent());
        }
    }

    /**
     * Notify an error. The channel source implementation can use this method to
     * tell the gpclient that an error has occurred that may make the channel
     * not work correctly.
     * 
     * @param error the error to notify; can't be null 
     */
    public void notifyError(Exception error) {
        if (error == null) {
            throw new IllegalArgumentException("The error to notify can't be null");
        }
        
        Consumer<PVEvent> listener;
        synchronized (lock) {
            listener = collectorListener;
        }
        // Run the task without holding the lock
        if (listener != null) {
            listener.accept(PVEvent.exceptionEvent(error));
        }
    }
    
}
