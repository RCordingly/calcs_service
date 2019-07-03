package lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import faasinspector.Inspector;
import java.util.HashMap;
import java.util.Random;

/**
 * uwt.lambda_test::handleRequest
 *
 * @author Wes Lloyd
 * @author Robert Cordingly
 */
public class Hello implements RequestHandler<Request, HashMap<String, Object>> {

    /**
     * Lambda Function Handler
     *
     * @param request Request POJO with defined variables from Request.java
     * @param context
     * @return HashMap that Lambda will automatically convert into JSON.
     */
    public HashMap<String, Object> handleRequest(Request request, Context context) {

        //Collect data
        Inspector inspector = new Inspector();
        inspector.inspectAll();
        inspector.addTimeStamp("frameworkRuntime");
        
        int threads = request.getThreads() - 1;
        int calcs = request.getCalcs();
        int sleep = request.getSleep();
        int loops = request.getLoops();
        
        //Kick off secondary threads to process math.
        for (int i = 0; i < threads - 1; i++) {
            (new Thread(new calcThread(calcs, sleep, loops))).start();
        }
        
        //Use this thread to do some math too.
        calcThread calculator = new calcThread(calcs, sleep, loops);
        calculator.run();
        
        inspector.addAttribute("finalCalc", calculator.lastCalc);
        
        inspector.inspectCPUDelta();
        return inspector.finish();
    }

    /**
     * Threads that does all of the math.
     */
    private class calcThread implements Runnable {

        private final int calcs;
        private final int sleep;
        private final int loops;
        
        private long lastCalc = 0;
        
        //Set seed so random always returns the same set of values.
        Random rand = new Random(42);

        private calcThread(int calcs, int sleep, int loops) {
            this.calcs = calcs;
            this.sleep = sleep;
            this.loops = loops;
        }

        @Override
        public void run() {
            
            if (loops > 0) {
                for (int i = 0; i < loops; i++) {
                    lastCalc = (long) randomMath(calcs);
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException ie) {
                        System.out.println("Sleep was interrupted - calc mode...");
                    }
                }
            } else {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException ie) {
                    System.out.println("Sleep was interrupted - no calc mode...");
                }
            }
        }

        private double randomMath(int calcs) {
            // By not reusing the same variables in the calc, this should prevent
            // compiler optimization... Also each math operation should operate
            // on between operands in different memory locations.
            long mult;
            double div1 = 0;

            int a, b, c;

            for (int i = 0; i < calcs; i++) {
                // By not using sequential locations in the array, we should 
                // reduce memory lookup efficiency
                int j = rand.nextInt(calcs);

                a = i + 75;
                b = i + 256;
                c = i + 45;

                mult = a * b;
                div1 = (double) mult / (double) c;
            }
            return div1;
        }
    }
}
