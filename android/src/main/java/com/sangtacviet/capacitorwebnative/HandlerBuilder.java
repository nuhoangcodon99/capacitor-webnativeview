package com.sangtacviet.capacitorwebnative;

import android.content.Context;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.android.AndroidClassLoadingStrategy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class HandlerBuilder {
    private Context context;

    public void setContext(Context context) {
        this.context = context;
    }

    public Object createHandler(Class clazz, HandlerAction action, String target) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        try {
            ClassLoadingStrategy strategy = new AndroidClassLoadingStrategy.Wrapping(context.getDir(
                    "generated",
                    Context.MODE_PRIVATE));
            var b = new ByteBuddy().subclass(clazz)
                    .method((ElementMatcher<MethodDescription>) methodDescription -> target.contains(methodDescription.getName()))
                    .intercept(InvocationHandlerAdapter.of(new InvocationHandler() {
                        @Override
                        public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                            action.run(method);
                            return null;
                        }
                    }))
                    .make()
                    .load(clazz.getClassLoader(), strategy)
                    .getLoaded().getConstructor().newInstance();
            return clazz.cast(b);
        }catch (Exception e){
            return e;
        }
    }
    public Object createEventListener(Class clazz, String eventName, String target ,com.getcapacitor.Bridge bridge) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        var handler = new HandlerAction(){
            @Override
            public void run(Method method){
                bridge.triggerDocumentJSEvent(eventName, "'"+method.getName()+"'");
            }
        };
        var obj = this.createHandler(clazz, handler, target);
        return obj;
    }
    public abstract class HandlerAction{
        public void run(Method method){}
    }
}
