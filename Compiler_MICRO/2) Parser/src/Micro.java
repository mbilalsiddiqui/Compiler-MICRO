import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Stack;

class CustomErrorStrategy extends DefaultErrorStrategy {
        @Override
	public void reportError (Parser recognizer, RecognitionException e) {
            recover(recognizer, e);
	}
   
        @Override
        public void recover(Parser recognizer, RecognitionException e) {
            throw e;
        }
}
public class Micro{
    public static void main(String[] args) throws Exception {
	int tokenType = 0;
        String inputFile = null;
        if ( args.length>0 ) inputFile = args[0];
        InputStream is = System.in;
        if ( inputFile!=null ) {
            is = new FileInputStream(inputFile);
        }
        
        ANTLRInputStream input = new ANTLRInputStream(is);
        MicroLexer lexer = new MicroLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        try {
           MicroParser parser = new MicroParser(tokens);
           ANTLRErrorStrategy es = new CustomErrorStrategy();
           parser.setErrorHandler(es);
					// what these functions are doing??
           parser.program();
          // If Exception thrown, program will not reach the next statement. and will go to catch block.
           System.out.println("Accepted");
        }
        catch(Exception err) {
	   			System.out.println("Not Accepted");
        }
        
            
    }

    

}
