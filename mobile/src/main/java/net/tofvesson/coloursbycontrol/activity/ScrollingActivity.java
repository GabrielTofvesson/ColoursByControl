package net.tofvesson.coloursbycontrol.activity;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.EditText;
import android.widget.Toast;
import net.tofvesson.coloursbycontrol.CodeCtx;
import net.tofvesson.coloursbycontrol.R;
import net.tofvesson.coloursbycontrol.view.StackPushCard;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Stack;

public class ScrollingActivity extends AppCompatActivity {

    static{
        System.loadLibrary("lib-tlang");
        System.loadLibrary("lib-tlang-cbc");
    }

    public static Class<?> loadClass(byte[] code, ClassLoader loadInto) throws InvocationTargetException
    {
        try {
            Method m = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
            m.setAccessible(true); // Make sure we can invoke the method
            return (Class<?>) m.invoke(loadInto, code, 0, code.length);
        }
        // An exception should only be thrown if the bytecode is invalid
        // or a class with the same name is already loaded
        catch (NoSuchMethodException e) { throw new RuntimeException(e); }
        catch (IllegalAccessException e){ throw new RuntimeException(e); }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        final CodeCtx ctx = new CodeCtx("RunMe",
                new String[]{ "Hello World", "makeText", Toast.class.getName(), "show" }, // Constant pool
                new byte[] // Instructions
                        {
                                -127 , // 10000001 (Return: true, Params: 1)
                                12, 0,
                                2,  0,
                                12, 0,
                                0,
                                2, 1,
                                2, 2,
                                3, 3,
                                2, 3,
                                2, 2,
                                3, 0,
                                2, 0
                        }
        );
        System.out.println(ctx.eval(new Stack<>(), new Object[]{ this }));



        final StackPushCard cnst = (StackPushCard) findViewById(R.id.stackPush);
        cnst.push = "Hello World";
        cnst.setOnClickListener(v -> {
            final Dialog d = new Dialog(new android.view.ContextThemeWrapper(ScrollingActivity.this, R.style.AppThemeLight));
            d.setContentView(R.layout.dialog_constant);
            d.setTitle("Constant value");
            if(cnst.push!=null) ((EditText) d.findViewById(R.id.newConst)).setText(String.valueOf(cnst.push));
            d.findViewById(R.id.cancel_action).setOnClickListener(v12 -> d.dismiss());
            d.findViewById(R.id.confirm_action).setOnClickListener(v1 -> {
                cnst.push = ((EditText) d.findViewById(R.id.newConst)).getText();
                d.dismiss();
            });
            d.show();
        });
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> {

        });
        findViewById(R.id.fab1).setOnClickListener(v -> Toast.makeText(getApplicationContext(), "I'm going to analyze the defined stack to check whether or not there will be and error on execution", Toast.LENGTH_LONG).show());
    }

    private static void toast(Object o){ ExternalData d = (ExternalData) o; Toast.makeText(d.ctx, d.message, Toast.LENGTH_SHORT).show(); }
    private static void snackbar(Object o){ ExternalData d = (ExternalData) o; Snackbar.make(d.ctx.findViewById(android.R.id.content), d.message, Snackbar.LENGTH_LONG).show(); }

    static class ExternalData{ public String message; public Activity ctx; }
}
