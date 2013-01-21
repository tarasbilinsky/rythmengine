package com.greenlaw110.rythm.internal.dialect;

import com.greenlaw110.rythm.internal.CodeBuilder;
import com.greenlaw110.rythm.logger.ILogger;
import com.greenlaw110.rythm.logger.Logger;
import com.greenlaw110.rythm.spi.IContext;
import com.greenlaw110.rythm.spi.IDialect;
import com.greenlaw110.rythm.spi.IParserFactory;
import com.greenlaw110.rythm.utils.S;

import java.util.*;

public class DialectManager {
    protected ILogger logger = Logger.get(DialectManager.class);
    final static IDialect[] defDialects = {
        BasicRythm.INSTANCE,
        SimpleRythm.INSTANCE,
        Rythm.INSTANCE
    };
    public static IDialect getById(String id) {
        for (IDialect d : defDialects) {
            if (S.isEqual(id, d.id())) return d;
        }
        return null;
    }
    private static int indexOf(IDialect dialect) {
        if (null == dialect) throw new NullPointerException();
        for (int i = 0; i < defDialects.length; ++i) {
            if (S.isEqual(dialect.id(), defDialects[i].id())) return i;
        }
        return -1;
    }
    private static IDialect nextAvailable(IDialect d) {
        if (null == d) return defDialects[0];
        int i = indexOf(d) + 1;
        if (i >= defDialects.length) return null;
        return defDialects[i];
    }
    private static final InheritableThreadLocal<Stack<IDialect>> cur = new InheritableThreadLocal<Stack<IDialect>>() {
        @Override
        protected Stack<IDialect> initialValue() {
            return new Stack<IDialect>();
        }
    };
    public static void push(IDialect dialect) {
        if (null == dialect) throw new NullPointerException();
        cur.get().push(dialect);
    }
    public static IDialect current() {
        return cur.get().peek();
    }
    public static IDialect pop() {
        return cur.get().pop();
    }
    public void beginParse(IContext ctx) {
        CodeBuilder cb = ctx.getCodeBuilder();
        IDialect d = cb.requiredDialect;
        if (null == d) {
            d = ctx.getDialect();
            if (null != d) {
                // try the next available
                d = nextAvailable(d);
                if (null == d) throw new NullPointerException("No dialect can process the template");
            } else {
                // guess the most capable
                String template = ctx.getRemain();
                for (IDialect d0: defDialects) {
                    if (d0.isMyTemplate(template)) {
                        d = d0;
                        break;
                    }
                }
            }
        }
        ctx.setDialect(d);
        push(d);
        d.begin(ctx);
    }

    public void endParse(IContext ctx) {
        IDialect d = ctx.getDialect();
        d.end(ctx);
        pop();
    }

    private void registerParserFactories(IDialect dialect, IParserFactory ... factories) {
        for (IParserFactory pf: factories) {
            dialect.registerParserFactory(pf);
        }
    }
    public void registerExternalParsers(String dialectId, IParserFactory... factories) {
        if (null == dialectId) {
            for (IDialect d: defDialects) {
                registerParserFactories(d, factories);
            }
        } else {
            IDialect d = getById(dialectId);
            if (null != d) {
                registerParserFactories(d, factories);
            } else {
                throw new IllegalArgumentException("Cannot find dialect by Id: " + dialectId);
            }
        }
    }
}
