/**
 * Copyright (c) 2016 Red Hat.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hono.adapter.rest;

public interface PayloadEncoder {

    byte[] encode(Object payload);

    Object decode(byte[] payload);

}
