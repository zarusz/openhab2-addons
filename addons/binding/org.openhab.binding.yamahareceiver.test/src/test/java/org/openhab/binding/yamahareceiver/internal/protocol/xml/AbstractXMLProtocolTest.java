/**
 * Copyright (c) 2014,2018 by the respective copyright holders.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.yamahareceiver.internal.protocol.xml;

import org.junit.Before;
import org.mockito.Mock;

import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Baseline for tests for the XML protocol implementation.
 */
public abstract class AbstractXMLProtocolTest {

    @Mock
    protected XMLConnection con;

    protected ModelContext ctx;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ctx = new ModelContext(con);

        onSetUp();
    }

    protected void onSetUp() throws Exception {
    }

}
