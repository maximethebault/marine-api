/*
 * SentenceReader.java
 * Copyright (C) 2010-2014 Kimmo Tuukkanen
 *
 * This file is part of Java Marine API.
 * <http://ktuukkan.github.io/marine-api/>
 *
 * Java Marine API is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Java Marine API is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Java Marine API. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.marineapi.nmea.io;

import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.SentenceId;
import net.sf.marineapi.nmea.sentence.SentenceValidator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sentence reader detects supported NMEA 0183 sentences from the specified
 * data source and dispatches them to registered listeners as sentence events.
 * Each event contains a parser for the read sentence.
 * <p/>
 * Parsers dispatched by reader are created using {@link net.sf.marineapi.nmea.parser.SentenceFactory} class,
 * where you can also register your own custom parsers.
 *
 * @author Kimmo Tuukkanen
 * @see net.sf.marineapi.nmea.event.AbstractSentenceListener
 * @see net.sf.marineapi.nmea.event.SentenceListener
 * @see net.sf.marineapi.nmea.event.SentenceEvent
 * @see net.sf.marineapi.nmea.parser.SentenceFactory
 */
public class StringSentenceReader {

    // Map key for listeners that listen any kind of sentences, type
    // specific listeners are registered with sentence type String
    private static final String DISPATCH_ALL = "DISPATCH_ALL";

    // logging
    private static final Logger LOGGER = Logger.getLogger(StringSentenceReader.class.getName());
    private static final String LOG_MSG = "Exception caught from SentenceListener";

    // map of sentence listeners
    private ConcurrentMap<String, List<SentenceListener>> listeners = new ConcurrentHashMap<String, List<SentenceListener>>();
    // Non-NMEA data listener
    private DataListener dataListener;
    // Exception listener
    private ExceptionListener exceptionListener = null;

    /**
     * Adds a {@link net.sf.marineapi.nmea.event.SentenceListener} that wants to receive all sentences read
     * by the reader.
     *
     * @param listener
     *         {@link net.sf.marineapi.nmea.event.SentenceListener} to be registered.
     *
     * @see net.sf.marineapi.nmea.event.SentenceListener
     */
    public void addSentenceListener(SentenceListener listener) {
        registerListener(listener, DISPATCH_ALL);
    }

    /**
     * Adds a {@link net.sf.marineapi.nmea.event.SentenceListener} that is interested in receiving only
     * sentences of certain type.
     *
     * @param sl
     *         SentenceListener to add
     * @param type
     *         Sentence type for which the listener is registered.
     *
     * @see net.sf.marineapi.nmea.event.SentenceListener
     */
    public void addSentenceListener(SentenceListener sl, SentenceId type) {
        registerListener(sl, type.toString());
    }

    /**
     * Adds a {@link net.sf.marineapi.nmea.event.SentenceListener} that is interested in receiving only
     * sentences of certain type.
     *
     * @param sl
     *         SentenceListener to add
     * @param type
     *         Sentence type for which the listener is registered.
     *
     * @see net.sf.marineapi.nmea.event.SentenceListener
     */
    public void addSentenceListener(SentenceListener sl, String type) {
        registerListener(sl, type);
    }

    /**
     * Returns all currently registered SentenceListeners.
     *
     * @return List of SentenceListeners or empty list.
     */
    List<SentenceListener> getSentenceListeners() {
        Set<SentenceListener> all = new HashSet<SentenceListener>();
        for (List<SentenceListener> sl : listeners.values()) {
            all.addAll(sl);
        }
        return new ArrayList<SentenceListener>(all);
    }

    /**
     * Dispatch data to all listeners.
     *
     * @param sentence
     *         sentence string.
     */
    void fireSentenceEvent(Sentence sentence) {

        String type = sentence.getSentenceId();
        Set<SentenceListener> targets = new HashSet<SentenceListener>();

        if (listeners.containsKey(type)) {
            targets.addAll(listeners.get(type));
        }
        if (listeners.containsKey(DISPATCH_ALL)) {
            targets.addAll(listeners.get(DISPATCH_ALL));
        }

        for (SentenceListener listener : targets) {
            try {
                SentenceEvent se = new SentenceEvent(this, sentence);
                listener.sentenceRead(se);
            }
            catch (Exception e) {
                LOGGER.log(Level.WARNING, LOG_MSG, e);
            }
        }
    }

    /**
     * Pass data to DataListener.
     */
    void fireDataEvent(String data) {
        try {
            if (dataListener != null) {
                dataListener.dataRead(data);
            }
        }
        catch (Exception e) {

        }
    }

    /**
     * Returns the exception call-back listener.
     *
     * @return Currently set ExceptionListener, or <code>null</code> if none.
     */
    public ExceptionListener getExceptionListener() {
        return exceptionListener;
    }

    /**
     * Set exception call-back listener.
     *
     * @param exceptionListener
     *         Listener to set, or <code>null</code> to reset.
     */
    public void setExceptionListener(ExceptionListener exceptionListener) {
        this.exceptionListener = exceptionListener;
    }

    /**
     * Handles an exception by passing it to ExceptionHandler. If no handler
     * is present, logs the error at level WARNING.
     *
     * @param msg
     *         Error message for logging
     * @param ex
     *         Exception to handle
     */
    void handleException(String msg, Exception ex) {
        if (exceptionListener == null) {
            LOGGER.log(Level.WARNING, msg, ex);
        }
        else {
            try {
                exceptionListener.onException(ex);
            }
            catch (Exception e) {
                LOGGER.log(Level.WARNING, "Exception thrown by ExceptionListener", e);
            }
        }
    }

    /**
     * Registers a SentenceListener to hash map with given key.
     *
     * @param listener
     *         SentenceListener to register
     * @param type
     *         Sentence type to register for
     */
    private void registerListener(SentenceListener listener, String type) {
        if (listeners.containsKey(type)) {
            listeners.get(type).add(listener);
        }
        else {
            List<SentenceListener> list = new Vector<SentenceListener>();
            list.add(listener);
            listeners.put(type, list);
        }
    }

    /**
     * Remove a listener from reader. When removed, listener will not receive
     * any events from the reader.
     *
     * @param listener
     *         {@link net.sf.marineapi.nmea.event.SentenceListener} to be removed.
     */
    public void removeSentenceListener(SentenceListener listener) {
        for (List<SentenceListener> list : listeners.values()) {
            if (list.contains(listener)) {
                list.remove(listener);
            }
        }
    }

    /**
     * Set listener for any data that is not recognized as NMEA 0183.
     * devices and environments that produce mixed content with both NMEA and
     * non-NMEA data.
     *
     * @param listener
     *         Listener to set, <code>null</code> to remove.
     */
    public void setDataListener(DataListener listener) {
        this.dataListener = listener;
    }

    /**
     * Reads a string
     */
    public void read(String data) {
        SentenceFactory factory = SentenceFactory.getInstance();

        if (SentenceValidator.isValid(data)) {
            Sentence s = factory.createParser(data);
            fireSentenceEvent(s);
        }
        else if (!SentenceValidator.isSentence(data)) {
            fireDataEvent(data);
        }
    }
}
