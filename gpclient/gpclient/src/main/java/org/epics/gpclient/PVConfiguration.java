/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.epics.gpclient;

import java.time.Duration;
import java.util.concurrent.Executor;
import org.epics.gpclient.datasource.DataSource;

/**
 * Allows to configure the type of read/write PV to create.
 *
 * @param <R> the read payload
 * @param <W> the write payload
 * @author carcassi
 */
public class PVConfiguration<R, W> implements PVReaderConfiguration<R>, PVWriterConfiguration<W> {
    
    static enum Mode {READ, WRITE, READ_WRITE};

    final Expression<R, W> expression;
    final GPClientInstance gpClient;
    final Mode mode;
    
    Executor notificationExecutor;
    DataSource dataSource;
    Duration connectionTimeout;
    String connectionTimeoutMessage;
    Duration maxRate;
    PVListener<R, W> listener;

    PVConfiguration(GPClientInstance gpClient, Expression<R, W> expression, Mode mode) {
        this.gpClient = gpClient;
        this.expression = expression;
        this.mode = mode;
    }

    /**
     * Defines which DataSource should be used to read the data.
     *
     * @param dataSource a connection manager
     * @return this
     */
    public PVConfiguration<R, W> from(DataSource dataSource) {
        // TODO: check all parameters for double setting
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource can't be null");
        }
        this.dataSource = dataSource;
        return this;
    }

    /**
     * Defines on which thread the PVManager should notify the client.
     *
     * @param onThread the thread on which to notify
     * @return this
     */
    public PVConfiguration<R, W> notifyOn(Executor onThread) {
        if (this.notificationExecutor == null) {
            this.notificationExecutor = onThread;
        } else {
            throw new IllegalStateException("Already set what thread to notify");
        }
        return this;
    }
    
    public PVConfiguration<R, W> maxRate(Duration maxRate) {
        if (this.maxRate != null)
            throw new IllegalStateException("Max rate already set");
        GPClientGlobalChecks.validateMaxRate(maxRate);
        this.maxRate = maxRate;
        return this;
    }
    
    public PVConfiguration<R, W> connectionTimeout(Duration timeout) {
        if (this.connectionTimeout != null)
            throw new IllegalStateException("Timeout already set");
        this.connectionTimeout = timeout;
        return this;
    }
    
    public PVConfiguration<R, W> connectionTimeout(Duration timeout, String timeoutMessage) {
        connectionTimeout(timeout);
        this.connectionTimeoutMessage = timeoutMessage;
        return this;
    }

    void checkParameters() {
        // Get defaults
        if (dataSource == null) {
            dataSource = gpClient.defaultDataSource;
        }
        if (notificationExecutor == null) {
            notificationExecutor = gpClient.defaultNotificationExecutor;
        }
        
        if (maxRate == null) {
            maxRate = gpClient.defaultMaxRate;
        }

        if (connectionTimeoutMessage == null)
            connectionTimeoutMessage = "Connection timeout";
        
        // Check that a data source has been specified
        if (dataSource == null) {
            throw new IllegalStateException("You need to specify a source either "
                    + "using PVManager.setDefaultDataSource or by using "
                    + "read(...).from(dataSource).");
        }

        // Check that thread switch has been specified
        if (notificationExecutor == null) {
            throw new IllegalStateException("You need to specify a thread either "
                    + "using PVManager.setDefaultThreadSwitch or by using "
                    + "read(...).andNotify(threadSwitch).");
        }
        
        // Check that a listener has been specified
        if (listener == null) {
            throw new IllegalStateException("You need to specify a listener "
                    + "by using "
                    + "read(...).listener(listener).");
        }
    }
    
    public PVConfiguration<R, W>  addListener(final PVReaderListener<R> listener) {
        final PVListener<R, W> previousListener = this.listener;
        this.listener = new PVListener<R, W>() {
            @Override
            public void pvChanged(PVEvent event, PV<R, W> pvReader) {
                if (previousListener != null) {
                    previousListener.pvChanged(event, pvReader);
                }
                listener.pvChanged(event, pvReader);
            }
        };
        return this;
    }

    public PVConfiguration<R, W> addListener(PVWriterListener<W> listener) {
        final PVListener<R, W> previousListener = this.listener;
        this.listener = new PVListener<R, W>() {
            @Override
            public void pvChanged(PVEvent event, PV<R, W> pvReader) {
                if (previousListener != null) {
                    previousListener.pvChanged(event, pvReader);
                }
                listener.pvChanged(event, pvReader);
            }
        };
        return this;
    }

    public PVConfiguration<R, W> addListener(PVListener<R, W> listener) {
        if (this.listener == null) {
            this.listener = listener;
        } else {
            final PVListener<R, W> previousListener = this.listener;
            this.listener = new PVListener<R, W>() {
                @Override
                public void pvChanged(PVEvent event, PV<R, W> pv) {
                    previousListener.pvChanged(event, pv);
                    listener.pvChanged(event, pv);
                }
            };
        }
        return this;
    }
    
    @Override
    public PV<R, W> start() {
        checkParameters();
        PVImpl<R, W> pv = new PVImpl<R, W>(listener);
        
        PVDirector<R, W> pvDirector = new PVDirector<R, W>(pv, this);
        
        RateDecoupler rateDecoupler;
        if (pvDirector.readFunction instanceof ReadCollector.CollectorSupplier) {
            rateDecoupler = new PassiveRateDecoupler(pvDirector.scannerExecutor, pvDirector.maxRate, pvDirector.getDesiredRateEventListener(), null);
        } else {
            rateDecoupler = new ActiveRateDecoupler(pvDirector.scannerExecutor, pvDirector.maxRate, pvDirector.getDesiredRateEventListener(), null);
        }
        
        pv.setDirector(pvDirector);
        pvDirector.setScanner(rateDecoupler);
        switch(mode) {
            case READ:
                pvDirector.connectReadExpression(expression);
                break;
            case WRITE:
                pvDirector.connectWriteExpression(expression);
                break;
            case READ_WRITE:
                pvDirector.connectReadExpression(expression);
                pvDirector.connectWriteExpression(expression);
                break;
        }
        rateDecoupler.start();
        
        return pv;
    }
    
}
