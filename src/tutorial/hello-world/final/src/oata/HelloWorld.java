package oata;

import org.apache.log4j.Logger;

public class HelloWorld {
    static Logger logger = Logger.getLogger(HelloWorld.class);

    public static void main(String[] args) {
        logger.info("Hello World");          // the old System.out-statement
    }
}
