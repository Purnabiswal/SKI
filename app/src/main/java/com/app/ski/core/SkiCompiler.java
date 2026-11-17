package com.app.ski.core;

import com.app.ski.model.SkiFile;
import com.app.ski.model.SkiProject;

import java.util.ArrayList;
import java.util.List;

public class SkiCompiler {

    private static class FileBlock {
        String path;
        String content;

        FileBlock(String path, String content) {
            this.path = path;
            this.content = content;
        }
    }

    public static String compileFrontend(SkiProject project) {

        SkiFile frontend = project.getFile("frontend.ski");

        if (frontend == null) {
            SkiFile direct = project.getFile("index.html");

            if (direct != null) {
                return direct.getContent();
            }

            return "<!doctype html><html><body><h1>No frontend.ski</h1></body></html>";
        }

        String source = frontend.getContent();

        if (source == null || source.trim().isEmpty()) {
            return "<!doctype html><html><body><h1>Empty frontend.ski</h1></body></html>";
        }

        List<FileBlock> files = parseFileBlocks(source);

        /*
         * If there are no @file blocks, treat the whole file
         * as normal HTML.
         */
        if (files.isEmpty()) {
            return source;
        }

        String html = null;

        StringBuilder css = new StringBuilder();
        StringBuilder js = new StringBuilder();

        for (FileBlock file : files) {

            String path = file.path.toLowerCase();
            String content = file.content.trim();

            /*
             * Supports:
             *
             * index.html
             * frontend/index.html
             * /frontend/index.html
             */
            if (path.endsWith("/index.html")
                    || path.equals("index.html")) {

                html = content;

            } else if (path.endsWith(".css")) {

                css.append(content).append("\n");

            } else if (path.endsWith(".js")) {

                js.append(content).append("\n");
            }
        }

        /*
         * If there was no index.html block, use the first
         * HTML file we can find.
         */
        if (html == null) {

            for (FileBlock file : files) {

                if (file.path.toLowerCase().endsWith(".html")) {
                    html = file.content.trim();
                    break;
                }
            }
        }

        if (html == null) {
            return "<!doctype html><html><body><h1>No HTML file found</h1></body></html>";
        }

        /*
         * Inject CSS into HTML.
         */
        if (css.length() > 0) {

            String style =
                    "<style>\n" +
                            css +
                            "\n</style>\n";

            html = injectBefore(
                    html,
                    "</head>",
                    style
            );
        }

        /*
         * Inject JavaScript into HTML.
         */
        if (js.length() > 0) {

            String script =
                    "<script>\n" +
                            js +
                            "\n</script>\n";

            html = injectBefore(
                    html,
                    "</body>",
                    script
            );
        }

        return html;
    }

    /**
     * Parses:
     *
     * @file "frontend/index.html" {
     *     ...
     * }
     *
     * using balanced braces instead of a regex.
     *
     * This allows JavaScript/CSS to contain nested {}.
     */
    private static List<FileBlock> parseFileBlocks(String source) {

        List<FileBlock> result = new ArrayList<>();

        int position = 0;

        while (position < source.length()) {

            int atFile = source.indexOf("@file", position);

            if (atFile < 0) {
                break;
            }

            int cursor = atFile + 5;

            // Skip whitespace
            while (cursor < source.length()
                    && Character.isWhitespace(source.charAt(cursor))) {
                cursor++;
            }

            // Expect opening quote
            if (cursor >= source.length()
                    || source.charAt(cursor) != '"') {

                position = cursor;
                continue;
            }

            cursor++;

            int pathStart = cursor;

            while (cursor < source.length()
                    && source.charAt(cursor) != '"') {
                cursor++;
            }

            if (cursor >= source.length()) {
                break;
            }

            String path = source.substring(pathStart, cursor);

            cursor++;

            // Skip whitespace before {
            while (cursor < source.length()
                    && Character.isWhitespace(source.charAt(cursor))) {
                cursor++;
            }

            if (cursor >= source.length()
                    || source.charAt(cursor) != '{') {

                position = cursor;
                continue;
            }

            int openBrace = cursor;

            int closeBrace = findMatchingBrace(
                    source,
                    openBrace
            );

            if (closeBrace < 0) {
                break;
            }

            String content = source.substring(
                    openBrace + 1,
                    closeBrace
            );

            result.add(
                    new FileBlock(path, content)
            );

            position = closeBrace + 1;
        }

        return result;
    }

    /**
     * Finds the matching } for an opening {.
     *
     * It understands:
     * - "strings"
     * - 'strings'
     * - `template strings`
     * - // comments
     * - /* comments *\/
     *
     * Therefore braces inside JavaScript strings/comments
     * do not incorrectly terminate the @file block.
     */
    private static int findMatchingBrace(
            String source,
            int openBrace
    ) {

        int depth = 0;

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;

        boolean inLineComment = false;
        boolean inBlockComment = false;

        boolean escaped = false;

        for (int i = openBrace; i < source.length(); i++) {

            char c = source.charAt(i);

            char next = i + 1 < source.length()
                    ? source.charAt(i + 1)
                    : '\0';

            /*
             * Line comment
             */
            if (inLineComment) {

                if (c == '\n' || c == '\r') {
                    inLineComment = false;
                }

                continue;
            }

            /*
             * Block comment
             */
            if (inBlockComment) {

                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }

                continue;
            }

            /*
             * Strings
             */
            if (inSingleQuote) {

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (c == '\\') {
                    escaped = true;
                    continue;
                }

                if (c == '\'') {
                    inSingleQuote = false;
                }

                continue;
            }

            if (inDoubleQuote) {

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (c == '\\') {
                    escaped = true;
                    continue;
                }

                if (c == '"') {
                    inDoubleQuote = false;
                }

                continue;
            }

            if (inBacktick) {

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (c == '\\') {
                    escaped = true;
                    continue;
                }

                if (c == '`') {
                    inBacktick = false;
                }

                continue;
            }

            /*
             * Start comments
             */
            if (c == '/' && next == '/') {

                inLineComment = true;
                i++;
                continue;
            }

            if (c == '/' && next == '*') {

                inBlockComment = true;
                i++;
                continue;
            }

            /*
             * Start strings
             */
            if (c == '\'') {
                inSingleQuote = true;
                continue;
            }

            if (c == '"') {
                inDoubleQuote = true;
                continue;
            }

            if (c == '`') {
                inBacktick = true;
                continue;
            }

            /*
             * Braces
             */
            if (c == '{') {

                depth++;

            } else if (c == '}') {

                depth--;

                if (depth == 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    private static String injectBefore(
            String html,
            String tag,
            String content
    ) {

        int index = html
                .toLowerCase()
                .lastIndexOf(tag.toLowerCase());

        if (index >= 0) {

            return html.substring(0, index)
                    + content
                    + html.substring(index);
        }

        return html + content;
    }
}