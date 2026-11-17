package com.app.ski.compiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkiCompiler {

    private final SkiParser parser;

    public SkiCompiler() {
        parser = new SkiParser();
    }

    public String compileFrontend(
            String frontendSource
    ) {

        List<SkiBlock> blocks =
                parser.parse(frontendSource);

        Map<String, String> files =
                new HashMap<>();

        for (SkiBlock block : blocks) {

            if (block.getType() != SkiBlock.Type.FILE) {
                continue;
            }

            if (files.containsKey(block.getPath())) {

                throw new IllegalArgumentException(
                        "Duplicate file: "
                                + block.getPath()
                );
            }

            files.put(
                    block.getPath(),
                    block.getContent()
            );
        }

        System.out.println(
                "SKI compiler files: " + files.keySet()
        );
        String html = files.get("index.html");

        if (html == null) {
            // Also support frontend/index.html for compatibility
            html = files.get("frontend/index.html");
        }

        if (html == null) {
            throw new IllegalArgumentException(
                    "index.html not found in frontend.ski"
            );
        }

        String css = files.get("style.css");

        if (css == null) {
            css = files.get("frontend/style.css");
        }

        String js = files.get("script.js");

        if (js == null) {
            js = files.get("frontend/script.js");
        }

        if (css != null) {

            html = injectCss(
                    html,
                    css
            );
        }

        if (js != null) {

            html = injectJs(
                    html,
                    js
            );
        }

        return html;
    }

    private String injectCss(
            String html,
            String css
    ) {

        String style =
                "<style>\n"
                        + css
                        + "\n</style>";

        if (
                html.contains("</head>")
        ) {

            return html.replace(
                    "</head>",
                    style + "\n</head>"
            );
        }

        return style + html;
    }

    private String injectJs(
            String html,
            String js
    ) {

        String script =
                "<script>\n"
                        + js
                        + "\n</script>";

        if (
                html.contains("</body>")
        ) {

            return html.replace(
                    "</body>",
                    script + "\n</body>"
            );
        }

        return html + script;
    }
}