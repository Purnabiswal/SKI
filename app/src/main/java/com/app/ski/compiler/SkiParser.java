package com.app.ski.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkiParser {

    private static final Pattern FILE_PATTERN =
            Pattern.compile(
                    "@file\\s+\"([^\"]+)\"\\s*\\{",
                    Pattern.MULTILINE
            );

    private static final Pattern ROUTE_PATTERN =
            Pattern.compile(
                    "@route\\s+(GET|POST|PUT|PATCH|DELETE|OPTIONS|HEAD)\\s+\"([^\"]+)\"\\s*\\{",
                    Pattern.MULTILINE
            );

    public List<SkiBlock> parse(String source) {

        if (source == null) {
            throw new IllegalArgumentException(
                    "SKI source cannot be null"
            );
        }

        List<SkiBlock> blocks =
                new ArrayList<>();

        int position = 0;

        while (position < source.length()) {

            while (
                    position < source.length()
                            && Character.isWhitespace(
                            source.charAt(position)
                    )
            ) {
                position++;
            }

            if (position >= source.length()) {
                break;
            }

            // Single-line comment
            if (source.startsWith("//", position)) {

                int end =
                        source.indexOf(
                                '\n',
                                position
                        );

                if (end == -1) {
                    break;
                }

                position = end + 1;
                continue;
            }

            // @file
            if (source.startsWith("@file", position)) {

                Matcher matcher =
                        FILE_PATTERN.matcher(
                                source.substring(position)
                        );

                if (!matcher.lookingAt()) {
                    throw new IllegalArgumentException(
                            "Invalid @file declaration near: "
                                    + source.substring(
                                    position,
                                    Math.min(
                                            position + 100,
                                            source.length()
                                    )
                            )
                    );
                }

                String path =
                        matcher.group(1);

                int bracePosition =
                        position + matcher.end() - 1;

                int end =
                        findMatchingBrace(
                                source,
                                bracePosition
                        );

                String content =
                        source.substring(
                                bracePosition + 1,
                                end
                        );

                validatePath(path);

                blocks.add(
                        new SkiBlock(
                                SkiBlock.Type.FILE,
                                path,
                                null,
                                content
                        )
                );

                position = end + 1;
                continue;
            }

            // @route
            if (source.startsWith("@route", position)) {

                Matcher matcher =
                        ROUTE_PATTERN.matcher(
                                source.substring(position)
                        );

                if (!matcher.lookingAt()) {
                    throw new IllegalArgumentException(
                            "Invalid @route declaration near: "
                                    + source.substring(
                                    position,
                                    Math.min(
                                            position + 100,
                                            source.length()
                                    )
                            )
                    );
                }

                String method =
                        matcher.group(1);

                String path =
                        matcher.group(2);

                int bracePosition =
                        position + matcher.end() - 1;

                int end =
                        findMatchingBrace(
                                source,
                                bracePosition
                        );

                String content =
                        source.substring(
                                bracePosition + 1,
                                end
                        ).trim();

                blocks.add(
                        new SkiBlock(
                                SkiBlock.Type.ROUTE,
                                path,
                                method,
                                content
                        )
                );

                position = end + 1;
                continue;
            }

            int end =
                    source.indexOf(
                            '\n',
                            position
                    );

            if (end == -1) {
                end = source.length();
            }

            String line =
                    source.substring(
                            position,
                            end
                    ).trim();

            throw new IllegalArgumentException(
                    "Unexpected syntax: " + line
            );
        }

        return blocks;
    }

    private int findMatchingBrace(
            String source,
            int openingBrace
    ) {

        int depth = 0;

        char quote = 0;

        boolean escaped = false;
        boolean lineComment = false;
        boolean blockComment = false;

        for (
                int i = openingBrace;
                i < source.length();
                i++
        ) {

            char c =
                    source.charAt(i);

            char next =
                    i + 1 < source.length()
                            ? source.charAt(i + 1)
                            : '\0';

            if (lineComment) {

                if (c == '\n') {
                    lineComment = false;
                }

                continue;
            }

            if (blockComment) {

                if (c == '*' && next == '/') {
                    blockComment = false;
                    i++;
                }

                continue;
            }

            if (quote != 0) {

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (c == '\\') {
                    escaped = true;
                    continue;
                }

                if (c == quote) {
                    quote = 0;
                }

                continue;
            }

            if (c == '/' && next == '/') {
                lineComment = true;
                i++;
                continue;
            }

            if (c == '/' && next == '*') {
                blockComment = true;
                i++;
                continue;
            }

            if (
                    c == '"'
                            || c == '\''
                            || c == '`'
            ) {
                quote = c;
                continue;
            }

            if (c == '{') {
                depth++;
            }

            if (c == '}') {

                depth--;

                if (depth == 0) {
                    return i;
                }
            }
        }

        throw new IllegalArgumentException(
                "Unmatched {"
        );
    }

    private void validatePath(String path) {

        if (
                path == null
                        || path.isEmpty()
                        || path.startsWith("/")
                        || path.contains("..")
        ) {
            throw new IllegalArgumentException(
                    "Unsafe virtual path: " + path
            );
        }
    }
}