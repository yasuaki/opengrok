/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */
package org.opensolaris.opengrok.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.opensolaris.opengrok.configuration.Project;

/**
 * The default access control instance that provides full access to
 * the system.
 * 
 * @author Trond Norbye <trond.norbye@gmail.com>
 */
public class NoAccessControl implements AccessControl {

    @Override
    public int canAccess(HttpServletRequest request) {
        return HttpServletResponse.SC_OK;
    }

    @Override
    public int canAccess(HttpServletRequest request, String path) {
        return HttpServletResponse.SC_OK;
    }

    @Override
    public int canAccess(HttpServletRequest request, Project project) {
        return HttpServletResponse.SC_OK;
    }
}
