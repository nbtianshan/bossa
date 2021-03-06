/*
 * Bossa Workflow System
 *
 * $Id$
 *
 * Copyright (C) 2003,2004 OpenBR Sistemas S/C Ltda.
 *
 * This file is part of Bossa.
 *
 * Bossa is free software; you can redistribute it and/or modify it
 * under the terms of version 2 of the GNU General Public License as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package com.bigbross.bossa.resource;

import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * A node of one binary tree representing a compiled resource expression. <p>
 * The set arithmetic operations are:
 * <ul>
 * <li> Union: <code>+</code>
 * <li> Intersection: <code>^</code>
 * <li> Exclusion: <code>-</code>
 * </ul>
 *
 * The operands are resource names. <br>
 * Whitespace characters are ignored. <br>
 * The expression is evaluated from left to right. <br>
 * Parenthesis may be used to group subexpressions. <p>
 *
 * The resource is linked from the <code>ResourceRegistry</code> at the
 * compilation time, and is created if needed. A <code>$</code> may be used to
 * delay the resource resolution to evaluation time, using a new
 * <code>ResourceRegistry</code>. <p>
 *
 * A resource expression example is: <code>sales - $request</code> than means
 * that only the users of the "sales" group minus the users that are in the
 * "request" group provided in the evaluation moment are in this role.
 *
 * @author <a href="http://www.bigbross.com">BigBross Team</a>
 * @see ResourceRegistry
 */
public abstract class Expression implements Container, Serializable {

    protected final static char OR  = '+';
    protected final static char SUB = '-';
    protected final static char AND = '^';
    protected final static char VAR = '$';
    protected final static char SPC = ' ';
    protected final static char LP  = '(';
    protected final static char RP  = ')';

    protected final static String DELIM = "" + OR + SUB + AND + VAR + SPC + LP + RP;

    /**
     * Compiles a resource expression. <p>
     *
     * @param registry a <code>ResourceRegistry</code> to link the resources
     *        in the expression.
     * @param expression the resource expression to be compiled.
     * @return a <code>Expression</code> representing the compiled resource
     *         expression.
     */
    public static Expression compile(ResourceRegistry registry, String expression) {
        StringTokenizer expr = new StringTokenizer(expression, DELIM, true);
        return compile(registry, expr, compile(registry, expr, null));
    }

    /**
     * Parses a resource expression building a binary tree. <p>
     *
     * @param registry a <code>ResourceRegistry</code> to link the resources
     *        in the expression.
     * @param expression the remaining resource expression to be compiled.
     * @param node a <code>Expression</code> value of the left node.
     * @return a <code>Expression</code> node of the compiled resource
     *         expression.
     */
    protected static Expression compile(ResourceRegistry registry, StringTokenizer expression, Expression node) {
        if (!expression.hasMoreTokens()) {
            return node;
        }

        String tok = expression.nextToken();

        while (tok.charAt(0) == SPC) {
            tok = expression.nextToken();
        }

        switch (tok.charAt(0)) {

        case OR: // Union node
            return compile(registry, expression, new Node(node, compile(registry, expression, node)) {

                    public boolean contains(ResourceRegistry registry, Resource resource) {
                        return left.contains(registry, resource) || right.contains(registry, resource);
                    }

                    public String toString() {
                        return toString(OR);
                    }
                });

        case AND: // Intersection node
            return compile(registry, expression, new Node(node, compile(registry, expression, node)) {

                    public boolean contains(ResourceRegistry registry, Resource resource) {
                        return left.contains(registry, resource) && right.contains(registry, resource);
                    }

                    public String toString() {
                        return toString(AND);
                    }
                });

        case SUB: // Subtraction node
            return compile(registry, expression, new Node(node, compile(registry, expression, node)) {

                    public boolean contains(ResourceRegistry registry, Resource resource) {
                        return !right.contains(registry, resource) && left.contains(registry, resource);
                    }

                    public String toString() {
                        return toString(SUB);
                    }
                });

        case LP: // Parenthesis node
            return compile(registry, expression, compile(registry, expression, node));

        case RP: // Parenthesis end
            return node;

        case VAR: // Resource weak reference
            return new LazyReference(registry, expression.nextToken());

        default: // Resource reference
            return new Reference(registry, tok);

        }

    }

    /**
     * Determines if a resource is contained in this. <p>
     *
     * @param resource the resource to be looked for.
     * @return <code>true</code> if the resource is found, <code>false</code> otherwise.
     */
    public boolean contains(Resource resource) {
        return contains(null, resource);
    }

    /**
     * Determines if a resource is contained in this. <p>
     *
     * @param registry a <code>ResourceRegistry</code> to resolve lazy referenced resource.
     * @param resource the resource to be looked for.
     * @return <code>true</code> if the resource is found, <code>false</code> otherwise.
     */
    public abstract boolean contains(ResourceRegistry registry, Resource resource);

}

abstract class Node extends Expression {

    protected Expression left;
    protected Expression right;

    protected Node(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Returns a string with the resource expression. <p>
     *
     * @return a string representation of this resource.
     */
    protected String toString(char op) {
        StringBuffer sb = new StringBuffer();

        if (left instanceof Node) {
            sb.append("(").append(left).append(")");
        } else {
            sb.append(left);
        }

        sb.append(op);

        if (right instanceof Node) {
            sb.append("(").append(right).append(")");
        } else {
            sb.append(right);
        }

        return sb.toString();
    }

}

class Reference extends Expression {

    Resource group;

    Reference(ResourceRegistry registry, String resource) {
        this.group = registry.getResource(resource);
        if (group == null) {
            this.group = registry.createResourceImpl(resource, false);
        }
    }

    public boolean contains(ResourceRegistry registry, Resource resource) {
        return group != null && group.contains(resource);
    }

    public String toString() {
        return String.valueOf(group);
    }

}

class LazyReference extends Expression {

    ResourceRegistry registry;
    String resourceId;

    LazyReference(ResourceRegistry registry, String resourceId) {
        this.registry = registry;
        this.resourceId = resourceId;
    }

    public boolean contains(ResourceRegistry context, Resource resource) {
        Resource group = (context == null) ? registry.getResource(resourceId) : context.getResource(resourceId);
        return group != null && group.contains(resource);
    }

    public String toString() {
        return VAR + resourceId;
    }

}
