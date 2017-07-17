package net.tofvesson.coloursbycontrol.activity;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import net.tofvesson.coloursbycontrol.CodeBuilder;
import net.tofvesson.coloursbycontrol.CodeContext;
import net.tofvesson.coloursbycontrol.ContextPool;
import net.tofvesson.coloursbycontrol.R;
import net.tofvesson.coloursbycontrol.view.ConstantCard;
import net.tofvesson.coloursbycontrol.view.FunctionCard;
import net.tofvesson.coloursbycontrol.view.LinearActionLayout;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Stack;
import static net.tofvesson.coloursbycontrol.Operations.*;

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


        CodeBuilder RunMe = new CodeBuilder(true, 1, "RunMe", "getToast", "makeText", Toast.class.getName(), "show");
        RunMe                               // Operations:                                                                                      |     Consumption     |  |    Addition     |  |  Delta  |   |Definite|
                .add(LDN /*, 0*/)           // Load 0 to the stack                                                                              (consumes 0 operand(s))  (adds 1 operand(s))  (impact: 1)   (stack: 1)
                .add(LDC /*, 0*/)           // Load constant at index 0 to the stack: "getToast"                                                (consumes 0 operand(s))  (adds 1 operand(s))  (impact: 1)   (stack: 2)
                .add(CALL)                  // Call context "getToast"                                                                          (consumes 1 operand(s))  (adds 1 operand(s))  (impact: 0)   (stack: 2)
                .add(LDN /*, 0*/)           // Load 0 to the stack                                                                              (consumes 0 operand(s))  (adds 1 operand(s))  (impact: 1)   (stack: 3)
                .add(LDV)                   // Load a variable to the stack: <Application context> (variable 0 is the first and only parameter) (consumes 1 operand(s))  (adds 1 operand(s))  (impact: 0)   (stack: 3)
                .add(LDC, 1)   // Load constant at index 1 to the stack: "makeText"                                                (consumes 0 operand(s))  (adds 1 operand(s))  (impact: 1)   (stack: 4)
                .add(LDC, 2)   // Load constant at index 2 to the stack: "android.widget.Toast"                                    (consumes 0 operand(s))  (adds 1 operand(s))  (impact: 1)   (stack: 5)
                .add(EXT, 3)   // Call external (Java) code: Toast.makeText(<Application context>, "Hello World", 0)               (consumes 5 operand(s))  (adds 1 operand(s))  (impact: -4)  (stack: 1)
                .add(LDC, 3)   // Load constant at index 3 to the stack: "show"                                                    (consumes 0 operand(s))  (adds 1 operand(s))  (impact: 1)   (stack: 2)
                .add(LDC, 2)   // Load constant at index 2 to the stack: "android.widget.Toast"                                    (consumes 0 operand(s))  (adds 1 operand(s))  (impact: 1)   (stack: 3)
                .add(EXT /*, 0*/)           // Call external (Java) code: <Toast Object>.show()                                                 (consumes 3 operand(s))  (adds 0 operand(s))  (impact: -3)  (stack: 0)
                .add(LDC /*, 0*/);          // Load constant at index 0 to the stack: "Hello World"                                             (consumes 0 operand(s))  (adds 1 operand(s))  (impact: 1)   (stack: 1)
                /* implicit return */       // Return from this subroutine to the super-routine (caller)                                        (consumes 1 operand[s])  (adds 0 operand(s))  (impact: -1)  (stack: 0)

        CodeBuilder getToast = new CodeBuilder(true, 0, "getToast", "Hey, World!");
        getToast.add(LDC);

        CodeBuilder toast = new CodeBuilder(false, 2, Toast.class.getName(), "show", "makeText");
        //TODO: Make CodeContext for showing toast

        ContextPool pool = new ContextPool();// Create a new pool (sandbox for execution)

        CodeContext ctx = pool.load(RunMe); // Load context "RunMe" to pool
        pool.load(getToast);                // Load context "getToast" to pool

        System.out.println(ctx.eval(new Stack<>(), this)); // Execute "RunMe"

        final ConstantCard cnst = (ConstantCard) findViewById(R.id.stackPush);
        cnst.push = "Hello World";
        cnst.setOnClickListener(v -> {
            final Dialog d = new Dialog(new android.view.ContextThemeWrapper(ScrollingActivity.this, R.style.AppThemeLight));
            d.setContentView(R.layout.dialog_constant);
            d.setTitle("Constant value");
            if(cnst.push!=null) ((EditText) d.findViewById(R.id.newConst)).setText(String.valueOf(cnst.push));
            d.findViewById(R.id.cancel_action).setOnClickListener(v12 -> d.dismiss());
            d.findViewById(R.id.confirm_action).setOnClickListener(v1 -> {
                cnst.push = ((EditText) d.findViewById(R.id.newConst)).getText().toString();
                d.dismiss();
            });
            d.show();
        });
        ((FunctionCard)findViewById(R.id.test)).instruction = "toast";
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            CodeBuilder builder = new CodeBuilder(false, 1, "exec");
            LinearActionLayout lal = (LinearActionLayout) findViewById(R.id.instructions);
            for(int i = 0; i<lal.getChildCount(); ++i) lal.getChildAt(i).processInstructions(builder);
            pool.load(builder).eval(new Stack<>(), this);
        });
        findViewById(R.id.fab1).setOnClickListener(v -> Toast.makeText(getApplicationContext(), "I'm going to analyze the defined stack to check whether or not there will be and error on execution", Toast.LENGTH_LONG).show());
    }
}
