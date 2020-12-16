package ru.makkarpov.scalingua.pofile.parse;

import java_cup.runtime.Symbol;
import java_cup.runtime.ComplexSymbolFactory.Location;
import java_cup.runtime.ComplexSymbolFactory.ComplexSymbol;

%%

%class PoLexer
%unicode
%line
%column
%char
%cup
%public

%{
    private String filename = "<unknown>";
    private StringBuilder string = new StringBuilder();
    private char commentTag = '\0';
    private Location storedLocation;

    private Location loc() {
        return new Location(filename, yyline, yycolumn, yychar);
    }

    private void storeLoc() {
        storedLocation = loc();
    }

    private Location storedLoc() {
        Location r = storedLocation;
        storedLocation = null;

        if (r == null)
            throw new RuntimeException("No stored location available");

        return r;
    }

    private Symbol sym(Location left, int id, Object arg) {
        Location right = new Location(filename, yyline, yycolumn + yylength(), yychar + yylength());
        return new ComplexSymbol(PoParserSym.terminalNames[id], id, left, right, arg);
    }

    private Symbol sym(int id, Object arg) {
        return sym(loc(), id, arg);
    }

    private Symbol sym(int id) {
        return sym(loc(), id, null);
    }

    private void lexerError(String msg) {
        throw new LexerException(loc(), msg);
    }

    private int extractNum() {
        return Integer.parseInt(yytext().replaceAll("[^0-9]+", ""));
    }

    private char extractEscape() {
        return (char) Integer.parseInt(yytext().replaceAll("[^0-9a-fA-F]+", ""), 16);
    }

    public PoLexer(java.io.Reader reader, String filename) {
        this(reader);
        this.filename = filename;
    }
%}

LineSeparator   = \r|\n|\r\n
InputCharacter  = [^\r\n]
WhiteSpace      = {LineSeparator}|\s
Digit           = [0-9]
HexDigit        = [0-9a-fA-F]

MsgId           = "msgid"
MsgIdPlural     = "msgid_plural"
MsgContext      = "msgctxt"
MsgString       = "msgstr"
MsgStringPlural = "msgstr[" {Digit}+ "]"
Comment         = "#"
CommentFirst    = \S|" "
UnicodeEscape   = \\u{HexDigit}{HexDigit}{HexDigit}{HexDigit}

%state STRING
%state LINE_START
%state COMMENT_START
%state COMMENT

%%

<YYINITIAL> {
    {MsgContext}        { return sym(PoParserSym.MSGCTXT); }
    {MsgId}             { return sym(PoParserSym.MSGID); }
    {MsgIdPlural}       { return sym(PoParserSym.MSGID_PLURAL); }
    {MsgString}         { return sym(PoParserSym.MSGSTR); }
    {MsgStringPlural}   { return sym(PoParserSym.MSGSTR_PLURAL, extractNum()); }

    // comments can occur only at the beginning of line
    ^{Comment}          { storeLoc(); yybegin(COMMENT_START); }
    {Comment}           { lexerError("Comments can only occur at the beginning of line"); }

    \"                  { storeLoc(); string.setLength(0); yybegin(STRING); }
    {WhiteSpace}        {}

    [^]                 { lexerError("Unrecognized token"); }
}

<STRING> {
    \"              { yybegin(YYINITIAL); return sym(storedLoc(), PoParserSym.STRING, string.toString()); }
    [^\r\n\"\\]+    { string.append(yytext()); }
    \\r             { string.append('\r'); }
    \\n             { string.append('\n'); }
    \\t             { string.append('\t'); }
    \\b             { string.append('\b'); }
    \\f             { string.append('\f'); }
    \\\\            { string.append('\\'); }
    \\\"            { string.append('"'); }
    \\'             { string.append('\''); }
    {UnicodeEscape} { string.append(extractEscape()); }
    \\              { lexerError("Unrecognized escape sequence"); }
    {LineSeparator} { lexerError("Unterminated string"); }
}

<COMMENT_START> {
    {CommentFirst}  { commentTag = yycharat(0); string.setLength(0); yybegin(COMMENT); }
    [^]             { lexerError("Invalid comment tag character"); }
}

<COMMENT> {
    [^\r\n]+        { string.append(yytext()); }
    {LineSeparator} { yybegin(YYINITIAL); return sym(storedLoc(), PoParserSym.COMMENT, new Comment(commentTag, string.toString())); }
}

<<EOF>>             { return sym(PoParserSym.EOF); }