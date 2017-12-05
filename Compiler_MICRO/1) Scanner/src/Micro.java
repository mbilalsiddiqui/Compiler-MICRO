import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Stack;

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
        for (Token token = lexer.nextToken(); token.getType() != Token.EOF; token = lexer.nextToken()){
         
          tokenType = token.getType();
					if (tokenType == 2)
					{
						System.out.println("Token Type: KEYWORD");
					}
					
					else if (tokenType == 3)
					{
						System.out.println("Token Type: OPERATOR");
					}

					else if (tokenType == 4)
					{
						System.out.println("Token Type: INTLITERAL");
					}

					else if (tokenType == 5)
					{
						System.out.println("Token Type: FLOATLITERAL");
					}
					
					else if (tokenType == 6)
					{
						System.out.println("Token Type: STRINGLITERAL");
						if (token.getText().length() > 80){
							System.out.println("StringLiteral exceeded 80 characters, lexer stopped");
							System.exit(-1);
						}
					}

					else if (tokenType == 7)
					{
						System.out.println("Token Type: COMMENT");
					}

					else if (tokenType == 8)
					{
						System.out.println("Token Type: WHITESPACE");
					}

					else if (tokenType == 9)
					{
						System.out.println("Token Type: IDENTIFIER");
						if (token.getText().length() > 31){
							System.out.println("Identifier exceeded 31 letters/numbers, lexer stopped");
							System.exit(-1);
						}
					}

					System.out.println("Value: "+token.getText());

        }

    }

}
