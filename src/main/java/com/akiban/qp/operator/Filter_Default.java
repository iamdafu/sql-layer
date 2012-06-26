/**
 * END USER LICENSE AGREEMENT (“EULA”)
 *
 * READ THIS AGREEMENT CAREFULLY (date: 9/13/2011):
 * http://www.akiban.com/licensing/20110913
 *
 * BY INSTALLING OR USING ALL OR ANY PORTION OF THE SOFTWARE, YOU ARE ACCEPTING
 * ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. YOU AGREE THAT THIS
 * AGREEMENT IS ENFORCEABLE LIKE ANY WRITTEN AGREEMENT SIGNED BY YOU.
 *
 * IF YOU HAVE PAID A LICENSE FEE FOR USE OF THE SOFTWARE AND DO NOT AGREE TO
 * THESE TERMS, YOU MAY RETURN THE SOFTWARE FOR A FULL REFUND PROVIDED YOU (A) DO
 * NOT USE THE SOFTWARE AND (B) RETURN THE SOFTWARE WITHIN THIRTY (30) DAYS OF
 * YOUR INITIAL PURCHASE.
 *
 * IF YOU WISH TO USE THE SOFTWARE AS AN EMPLOYEE, CONTRACTOR, OR AGENT OF A
 * CORPORATION, PARTNERSHIP OR SIMILAR ENTITY, THEN YOU MUST BE AUTHORIZED TO SIGN
 * FOR AND BIND THE ENTITY IN ORDER TO ACCEPT THE TERMS OF THIS AGREEMENT. THE
 * LICENSES GRANTED UNDER THIS AGREEMENT ARE EXPRESSLY CONDITIONED UPON ACCEPTANCE
 * BY SUCH AUTHORIZED PERSONNEL.
 *
 * IF YOU HAVE ENTERED INTO A SEPARATE WRITTEN LICENSE AGREEMENT WITH AKIBAN FOR
 * USE OF THE SOFTWARE, THE TERMS AND CONDITIONS OF SUCH OTHER AGREEMENT SHALL
 * PREVAIL OVER ANY CONFLICTING TERMS OR CONDITIONS IN THIS AGREEMENT.
 */

package com.akiban.qp.operator;

import com.akiban.qp.row.Row;
import com.akiban.qp.rowtype.RowType;
import com.akiban.sql.optimizer.explain.Explainer;
import com.akiban.sql.optimizer.explain.std.FilterExplainer;
import com.akiban.util.ArgumentValidation;
import com.akiban.util.tap.InOutTap;

import java.util.*;

/**

 <h1>Overview</h1>

 Extract_Default filters the input stream, keeping rows that match or are descendents of a given type, and discarding others.

 <h1>Arguments</h1>

 <ul>
 <li><b>Collection<RowType> extractTypes:</b> Specifies types of rows to be passed on to the output stream. 
</ul>
 
 <h1>Behavior</h1>

 A row is kept if its type matches one of the extractTypes, or is a descendent of one of these types. Other rows are discarded.

 <h1>Output</h1>

 Nothing else to say.

 <h1>Assumptions</h1>

 None.

 <h1>Performance</h1>

 Extract_Default does no IO. For each input row, the type is checked and the row is either kept (written to the output stream) or discarded.

 <h1>Memory Requirements</h1>

 None.

 
 */

class Filter_Default extends Operator
{
    // Object interface

    @Override
    public String toString()
    {
        TreeSet<String> keepTypesStrings = new TreeSet<String>();
        for (RowType keepType : keepTypes) {
            keepTypesStrings.add(String.valueOf(keepType));
        }
        return String.format("%s(%s)", getClass().getSimpleName(), keepTypesStrings);
    }

    // Operator interface

    @Override
    public void findDerivedTypes(Set<RowType> derivedTypes)
    {
        inputOperator.findDerivedTypes(derivedTypes);
    }

    @Override
    public List<Operator> getInputOperators()
    {
        return Collections.singletonList(inputOperator);
    }

    @Override
    protected Cursor cursor(QueryContext context)
    {
        return new Execution(context, inputOperator.cursor(context));
    }

    @Override
    public String describePlan()
    {
        return describePlan(inputOperator);
    }

    // Filter_Default interface

    public Filter_Default(Operator inputOperator, Collection<? extends RowType> keepTypes)
    {
        ArgumentValidation.notEmpty("keepTypes", keepTypes);
        this.inputOperator = inputOperator;
        this.keepTypes = new HashSet<RowType>(keepTypes);
    }
    
    // Class state
    
    private static final InOutTap TAP_OPEN = OPERATOR_TAP.createSubsidiaryTap("operator: Filter_Default open");
    private static final InOutTap TAP_NEXT = OPERATOR_TAP.createSubsidiaryTap("operator: Filter_Default next");

    // Object state

    private final Operator inputOperator;
    private final Set<RowType> keepTypes;

    @Override
    public Explainer getExplainer()
    {
        return new FilterExplainer("FILTER DEFAULT", keepTypes, inputOperator);
    }

    // Inner classes

    private class Execution extends OperatorExecutionBase implements Cursor
    {
        // Cursor interface

        @Override
        public void open()
        {
            TAP_OPEN.in();
            try {
                CursorLifecycle.checkIdle(this);
                input.open();
                closed = false;
            } finally {
                TAP_OPEN.out();
            }
        }

        @Override
        public Row next()
        {
            TAP_NEXT.in();
            try {
                CursorLifecycle.checkIdleOrActive(this);
                checkQueryCancelation();
                Row row;
                do {
                    row = input.next();
                    if (row == null) {
                        close();
                    } else if (!keepTypes.contains(row.rowType())) {
                        row = null;
                    }
                } while (row == null && !closed);
                return row;
            } finally {
                TAP_NEXT.out();
            }
        }

        @Override
        public void close()
        {
            CursorLifecycle.checkIdleOrActive(this);
            if (!closed) {
                input.close();
                closed = true;
            }
        }

        @Override
        public void destroy()
        {
            input.destroy();
        }

        @Override
        public boolean isIdle()
        {
            return closed;
        }

        @Override
        public boolean isActive()
        {
            return !closed;
        }

        @Override
        public boolean isDestroyed()
        {
            return input.isDestroyed();
        }

        // Execution interface

        Execution(QueryContext context, Cursor input)
        {
            super(context);
            this.input = input;
        }

        // Object state

        private final Cursor input;
        private boolean closed = true;
    }
}
