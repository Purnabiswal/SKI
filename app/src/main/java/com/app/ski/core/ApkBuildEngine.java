package com.app.ski.core;

import android.content.Context;
import android.content.res.AssetManager;

import com.app.ski.compiler.SkiCompiler;
import com.app.ski.model.BuildRecord;
import com.app.ski.model.SkiFile;
import com.app.ski.model.SkiProject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;


/**
 * Real on-device APK builder for SKI WEB / CROSS-PLATFORM projects.
 *
 * Architecture:
 *
 *     SKI project
 *          ↓
 *     compile frontend.ski
 *          ↓
 *     generate runtime files
 *          ↓
 *     copy WebView APK template
 *          ↓
 *     inject runtime files
 *          ↓
 *     create unsigned APK
 *          ↓
 *     APK alignment
 *          ↓
 *     APK signing
 *          ↓
 *     verify
 *          ↓
 *     save BuildRecord
 *
 * The APK template contains the already compiled
 * Android/WebView shell.
 *
 * We do NOT need to run Gradle on the phone.
 */
public class ApkBuildEngine {

    public interface Listener {

        void onProgress(
                int progress,
                String stage
        );

        void onSuccess(
                File apkFile,
                BuildRecord build
        );

        void onFailure(
                String error
        );

        void onCancelled();
    }


    private static final String TEMPLATE_APK =
            "ski_webview_template.apk";

    private static final String TEMPLATE_ASSET_ROOT =
            "assets/ski";


    private final Context context;

    private final BuildManager buildManager;

    private final BuildStatusManager statusManager;

    private final AtomicBoolean cancelled =
            new AtomicBoolean(false);


    public ApkBuildEngine(
            Context context,
            BuildManager buildManager,
            BuildStatusManager statusManager
    ) {

        this.context =
                context.getApplicationContext();

        this.buildManager =
                buildManager;

        this.statusManager =
                statusManager;
    }


    public void cancel() {

        cancelled.set(true);
    }


    /**
     * Starts a real APK build on a background thread.
     */
    public void build(
            SkiProject project,
            Listener listener
    ) {

        cancelled.set(false);

        new Thread(() -> {

            try {

                runBuild(
                        project,
                        listener
                );

            } catch (InterruptedException e) {

                Thread.currentThread()
                        .interrupt();

                statusManager.cancel();

                listener.onCancelled();

            } catch (Exception e) {

                String message =
                        e.getMessage();

                if (message == null ||
                        message.trim().isEmpty()) {

                    message =
                            "APK build failed";
                }

                e.printStackTrace();

                statusManager.failed(
                        message
                );

                listener.onFailure(
                        message
                );
            }

        }, "SKI-APK-Build").start();
    }


    /**
     * Complete APK build pipeline.
     */
    private void runBuild(
            SkiProject project,
            Listener listener
    ) throws Exception {

        if (project == null) {

            throw new IOException(
                    "No project selected."
            );
        }


        /*
         * ---------------------------------------------------------
         * 1. Start
         * ---------------------------------------------------------
         */

        update(
                2,
                "Starting APK build..."
        );

        checkCancelled();


        /*
         * ---------------------------------------------------------
         * 2. Fingerprint
         * ---------------------------------------------------------
         */

        update(
                5,
                "Calculating project fingerprint..."
        );

        String fingerprint =
                buildManager
                        .calculateProjectFingerprint(
                                project
                        );

        checkCancelled();


        /*
         * ---------------------------------------------------------
         * 3. Duplicate detection
         * ---------------------------------------------------------
         */

        update(
                8,
                "Checking existing APK builds..."
        );

        BuildRecord duplicate =
                buildManager.findDuplicate(
                        project.getName(),
                        BuildRecord.TYPE_APK,
                        fingerprint
                );

        if (duplicate != null) {

            throw new IOException(
                    "This exact project version has already been built."
            );
        }

        checkCancelled();


        /*
         * ---------------------------------------------------------
         * 4. Read project
         * ---------------------------------------------------------
         */

        update(
                12,
                "Reading SKI project..."
        );

        String frontend =
                findFileContent(
                        project,
                        "frontend.ski"
                );

        String backend =
                findFileContent(
                        project,
                        "backend.ski"
                );

        if (frontend == null) {

            throw new IOException(
                    "frontend.ski is missing."
            );
        }

        if (frontend.trim().isEmpty()) {

            throw new IOException(
                    "frontend.ski is empty."
            );
        }

        checkCancelled();


        /*
         * ---------------------------------------------------------
         * 5. Compile frontend
         * ---------------------------------------------------------
         */

        update(
                20,
                "Compiling frontend.ski..."
        );

        String compiledFrontend =
                compileFrontend(
                        frontend
                );

        checkCancelled();


        /*
         * ---------------------------------------------------------
         * 6. Prepare build directory
         * ---------------------------------------------------------
         */

        update(
                30,
                "Preparing Android build..."
        );

        File buildRoot =
                new File(
                        context.getCacheDir(),
                        "ski-apk-build"
                );

        deleteRecursive(
                buildRoot
        );

        if (!buildRoot.mkdirs() &&
                !buildRoot.exists()) {

            throw new IOException(
                    "Unable to create build directory."
            );
        }

        checkCancelled();


        /*
         * ---------------------------------------------------------
         * 7. Generate SKI runtime files
         * ---------------------------------------------------------
         */

        update(
                38,
                "Preparing WebView runtime..."
        );

        File runtimeDir =
                new File(
                        buildRoot,
                        "runtime"
                );

        if (!runtimeDir.mkdirs() &&
                !runtimeDir.exists()) {

            throw new IOException(
                    "Unable to create runtime directory."
            );
        }


        /*
         * frontend
         */
        writeText(
                new File(
                        runtimeDir,
                        "index.html"
                ),
                compiledFrontend
        );


        /*
         * backend
         */
        writeText(
                new File(
                        runtimeDir,
                        "backend.ski"
                ),
                backend == null
                        ? ""
                        : backend
        );


        /*
         * metadata
         */
        writeText(
                new File(
                        runtimeDir,
                        "ski-build.json"
                ),
                createMetadata(
                        project,
                        fingerprint
                )
        );

        checkCancelled();


        /*
         * ---------------------------------------------------------
         * 8. Copy WebView template
         * ---------------------------------------------------------
         */

        update(
                48,
                "Loading WebView APK template..."
        );

        File template =
                new File(
                        buildRoot,
                        "template.apk"
                );

        copyAsset(
                TEMPLATE_APK,
                template
        );

        if (!template.exists() ||
                template.length() == 0) {

            throw new IOException(
                    "WebView APK template is missing."
            );
        }

        checkCancelled();


        /*
         * ---------------------------------------------------------
         * 9. Create unsigned APK
         * ---------------------------------------------------------
         */

        update(
                58,
                "Injecting SKI application..."
        );

        File unsignedApk =
                new File(
                        buildRoot,
                        "unsigned.apk"
                );

        packageTemplate(
                template,
                runtimeDir,
                unsignedApk
        );

        if (!unsignedApk.exists() ||
                unsignedApk.length() == 0) {

            throw new IOException(
                    "Unsigned APK was not created."
            );
        }

        checkCancelled();


        /*
         * ---------------------------------------------------------
         * 10. Validate unsigned APK
         * ---------------------------------------------------------
         */

        update(
                66,
                "Validating unsigned APK..."
        );

        validateApk(
                unsignedApk
        );

        checkCancelled();


        /*
         * ---------------------------------------------------------
         * 11. Align APK
         *
         * IMPORTANT:
         *
         * Alignment MUST happen before signing.
         *
         * Android requires resources.arsc to be:
         *
         *     - uncompressed
         *     - 4-byte aligned
         *
         * This fixes:
         *
         * Targeting R+ requires resources.arsc
         * to be stored uncompressed and aligned.
         * ---------------------------------------------------------
         */

        update(
                74,
                "Aligning APK..."
        );

        File alignedApk =
                new File(
                        buildRoot,
                        "aligned.apk"
                );

        alignApk(
                unsignedApk,
                alignedApk
        );

        validateApk(
                alignedApk
        );

        checkCancelled();


        /*
         * ---------------------------------------------------------
         * 12. Create final output path
         * ---------------------------------------------------------
         */

        update(
                80,
                "Preparing final APK..."
        );

        File signedApk =
                createOutputApk(
                        project
                );


        /*
         * ---------------------------------------------------------
         * 13. Sign aligned APK
         * ---------------------------------------------------------
         */

        update(
                84,
                "Signing APK..."
        );

        ApkSigner.sign(
                context,
                alignedApk,
                signedApk
        );

        checkCancelled();


        /*
         * ---------------------------------------------------------
         * 14. Verify final signed APK
         * ---------------------------------------------------------
         */

        update(
                94,
                "Verifying signed APK..."
        );

        validateApk(
                signedApk
        );

        if (!signedApk.exists() ||
                signedApk.length() == 0) {

            throw new IOException(
                    "Signed APK was not created."
            );
        }

        checkCancelled();


        /*
         * ---------------------------------------------------------
         * 15. Register build
         * ---------------------------------------------------------
         */

        update(
                97,
                "Saving build record..."
        );

        BuildRecord build =
                buildManager.addBuild(
                        project.getName(),
                        project.getName(),
                        BuildRecord.TYPE_APK,
                        "1.0",
                        signedApk,
                        fingerprint
                );


        /*
         * ---------------------------------------------------------
         * 16. Success
         * ---------------------------------------------------------
         */

        update(
                100,
                "APK build complete"
        );

        statusManager.success(
                "APK created successfully"
        );

        listener.onSuccess(
                signedApk,
                build
        );
    }


    /**
     * Updates global build status.
     */
    private void update(
            int progress,
            String stage
    ) {

        statusManager.update(
                progress,
                stage
        );
    }


    /**
     * Checks whether the current build was cancelled.
     */
    private void checkCancelled()
            throws InterruptedException {

        if (cancelled.get()) {

            throw new InterruptedException(
                    "Build cancelled"
            );
        }
    }


    /**
     * Compiles frontend.ski using the real SKI compiler.
     */
    private String compileFrontend(
            String source
    ) throws Exception {

        if (source == null) {

            throw new IOException(
                    "frontend.ski is missing."
            );
        }

        if (source.trim().isEmpty()) {

            throw new IOException(
                    "frontend.ski is empty."
            );
        }

        try {

            SkiCompiler compiler =
                    new SkiCompiler();

            return compiler.compileFrontend(
                    source
            );

        } catch (Exception e) {

            String message =
                    e.getMessage();

            if (message == null ||
                    message.trim().isEmpty()) {

                message =
                        "Unknown frontend compilation error.";
            }

            throw new IOException(
                    "Unable to compile frontend.ski: "
                            + message,
                    e
            );
        }
    }


    /**
     * Finds a file inside the current SKI project.
     */
    private String findFileContent(
            SkiProject project,
            String filename
    ) {

        if (project == null ||
                project.getFiles() == null) {

            return null;
        }

        for (SkiFile file :
                project.getFiles()) {

            if (file == null) {
                continue;
            }

            if (filename.equalsIgnoreCase(
                    file.getName()
            )) {

                return file.getContent();
            }
        }

        return null;
    }


    /**
     * Creates metadata consumed by the SKI WebView runtime.
     */
    private String createMetadata(
            SkiProject project,
            String fingerprint
    ) {

        String projectName =
                project.getName();

        if (projectName == null) {
            projectName = "";
        }

        String escapedName =
                projectName
                        .replace(
                                "\\",
                                "\\\\"
                        )
                        .replace(
                                "\"",
                                "\\\""
                        );

        String safeFingerprint =
                fingerprint == null
                        ? ""
                        : fingerprint;

        return "{\n" +
                "  \"name\":\"" +
                escapedName +
                "\",\n" +
                "  \"fingerprint\":\"" +
                safeFingerprint +
                "\",\n" +
                "  \"engine\":\"SKI-WebView\",\n" +
                "  \"version\":\"1.0\"\n" +
                "}\n";
    }


    /**
     * Rebuilds the template APK while injecting
     * SKI runtime files.
     *
     * Old APK signature files are removed because
     * the resulting APK will receive a new signature.
     */
    private void packageTemplate(
            File template,
            File runtimeDir,
            File output
    ) throws IOException {

        byte[] buffer =
                new byte[8192];

        try (
                ZipFile zip =
                        new ZipFile(template);

                FileOutputStream fos =
                        new FileOutputStream(output);

                BufferedOutputStream bos =
                        new BufferedOutputStream(fos);

                ZipOutputStream zos =
                        new ZipOutputStream(bos)
        ) {

            Enumeration<? extends ZipEntry>
                    entries =
                    zip.entries();

            while (entries.hasMoreElements()) {

                ZipEntry oldEntry =
                        entries.nextElement();

                String name =
                        oldEntry.getName();


                /*
                 * Remove existing SKI runtime files.
                 */
                if (name.startsWith(
                        TEMPLATE_ASSET_ROOT + "/"
                )) {

                    continue;
                }


                /*
                 * Remove ALL old APK signature
                 * metadata.
                 *
                 * The resulting APK will be
                 * signed again.
                 */
                if (name.startsWith("META-INF/")) {

                    continue;
                }


                ZipEntry newEntry =
                        new ZipEntry(name);

                if (oldEntry.getTime() != -1) {

                    newEntry.setTime(
                            oldEntry.getTime()
                    );
                }


                /*
                 * Preserve STORED entries where possible.
                 *
                 * Other entries are written as
                 * normal ZIP entries.
                 */
                if (oldEntry.getMethod()
                        == ZipEntry.STORED) {

                    newEntry.setMethod(
                            ZipEntry.STORED
                    );

                    newEntry.setSize(
                            oldEntry.getSize()
                    );

                    newEntry.setCompressedSize(
                            oldEntry.getCompressedSize()
                    );

                    newEntry.setCrc(
                            oldEntry.getCrc()
                    );
                }


                zos.putNextEntry(
                        newEntry
                );


                try (
                        InputStream input =
                                new BufferedInputStream(
                                        zip.getInputStream(
                                                oldEntry
                                        )
                                )
                ) {

                    int count;

                    while (
                            (count =
                                    input.read(buffer))
                                    != -1
                    ) {

                        zos.write(
                                buffer,
                                0,
                                count
                        );
                    }
                }


                zos.closeEntry();
            }


            /*
             * Add new SKI runtime files.
             */
            addRuntimeFile(
                    zos,
                    runtimeDir,
                    "index.html"
            );

            addRuntimeFile(
                    zos,
                    runtimeDir,
                    "backend.ski"
            );

            addRuntimeFile(
                    zos,
                    runtimeDir,
                    "ski-build.json"
            );

            zos.finish();
        }
    }


    /**
     * Adds one runtime file under:
     *
     * assets/ski/<filename>
     */
    private void addRuntimeFile(
            ZipOutputStream zos,
            File runtimeDir,
            String filename
    ) throws IOException {

        File file =
                new File(
                        runtimeDir,
                        filename
                );

        if (!file.exists()) {

            throw new IOException(
                    "Runtime file missing: "
                            + filename
            );
        }


        ZipEntry entry =
                new ZipEntry(
                        TEMPLATE_ASSET_ROOT +
                                "/" +
                                filename
                );


        zos.putNextEntry(
                entry
        );


        try (
                InputStream input =
                        new BufferedInputStream(
                                new FileInputStream(file)
                        )
        ) {

            byte[] buffer =
                    new byte[8192];

            int count;

            while (
                    (count =
                            input.read(buffer))
                            != -1
            ) {

                zos.write(
                        buffer,
                        0,
                        count
                );
            }
        }


        zos.closeEntry();
    }


    /**
     * Validates the basic APK ZIP structure.
     */
    private void validateApk(
            File apk
    ) throws IOException {

        if (apk == null ||
                !apk.exists()) {

            throw new IOException(
                    "APK file was not created."
            );
        }


        if (apk.length() < 1024) {

            throw new IOException(
                    "Generated APK is invalid."
            );
        }


        try (
                ZipFile zip =
                        new ZipFile(apk)
        ) {

            ZipEntry manifest =
                    zip.getEntry(
                            "AndroidManifest.xml"
                    );

            if (manifest == null) {

                throw new IOException(
                        "APK does not contain AndroidManifest.xml."
                );
            }


            ZipEntry index =
                    zip.getEntry(
                            "assets/ski/index.html"
                    );

            if (index == null) {

                throw new IOException(
                        "APK does not contain SKI frontend."
                );
            }


            ZipEntry resources =
                    zip.getEntry(
                            "resources.arsc"
                    );

            if (resources == null) {

                throw new IOException(
                        "APK does not contain resources.arsc."
                );
            }


            /*
             * Android R+ requires resources.arsc
             * to be uncompressed.
             */
            if (resources.getMethod()
                    != ZipEntry.STORED) {

                throw new IOException(
                        "resources.arsc is compressed. "
                                + "APK alignment failed."
                );
            }
        }
    }


    /**
     * Creates the final APK path.
     *
     * Output:
     *
     * Android/data/<package>/files/SKI/builds/
     */
    private File createOutputApk(
            SkiProject project
    ) throws IOException {

        File externalFiles =
                context.getExternalFilesDir(null);

        if (externalFiles == null) {

            throw new IOException(
                    "External files directory is unavailable."
            );
        }


        File buildsDir =
                new File(
                        externalFiles,
                        "SKI/builds"
                );


        if (!buildsDir.exists()) {

            if (!buildsDir.mkdirs() &&
                    !buildsDir.exists()) {

                throw new IOException(
                        "Unable to create APK builds directory."
                );
            }
        }


        String safeName =
                sanitizeFilename(
                        project.getName()
                );


        return new File(
                buildsDir,
                safeName +
                        "-" +
                        System.currentTimeMillis() +
                        ".apk"
        );
    }


    /**
     * Makes a safe APK filename.
     */
    private String sanitizeFilename(
            String name
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return "SKI-App";
        }


        return name
                .replaceAll(
                        "[^a-zA-Z0-9._-]",
                        "_"
                );
    }


    /**
     * Copies an APK template from:
     *
     * app/src/main/assets/
     */
    private void copyAsset(
            String assetName,
            File destination
    ) throws IOException {

        AssetManager assets =
                context.getAssets();


        File parent =
                destination.getParentFile();

        if (parent != null &&
                !parent.exists()) {

            if (!parent.mkdirs() &&
                    !parent.exists()) {

                throw new IOException(
                        "Unable to create template directory."
                );
            }
        }


        try (
                InputStream input =
                        assets.open(assetName);

                FileOutputStream output =
                        new FileOutputStream(
                                destination
                        )
        ) {

            byte[] buffer =
                    new byte[8192];

            int count;

            while (
                    (count =
                            input.read(buffer))
                            != -1
            ) {

                output.write(
                        buffer,
                        0,
                        count
                );
            }
        }
    }


    /**
     * Writes UTF-8 text.
     */
    private void writeText(
            File file,
            String text
    ) throws IOException {

        File parent =
                file.getParentFile();

        if (parent != null &&
                !parent.exists()) {

            if (!parent.mkdirs() &&
                    !parent.exists()) {

                throw new IOException(
                        "Unable to create directory."
                );
            }
        }


        if (text == null) {
            text = "";
        }


        try (
                FileOutputStream output =
                        new FileOutputStream(file)
        ) {

            output.write(
                    text.getBytes(
                            StandardCharsets.UTF_8
                    )
            );
        }
    }


    /**
     * Deletes a file/directory recursively.
     */
    private void deleteRecursive(
            File file
    ) {

        if (file == null ||
                !file.exists()) {

            return;
        }


        if (file.isDirectory()) {

            File[] children =
                    file.listFiles();

            if (children != null) {

                for (File child :
                        children) {

                    deleteRecursive(
                            child
                    );
                }
            }
        }


        file.delete();
    }


    /**
     * APK alignment.
     *
     * Android requires resources.arsc to be:
     *
     * 1. STORED
     * 2. 4-byte aligned
     *
     * This method runs BEFORE APK signing.
     */
    private void alignApk(
            File inputApk,
            File outputApk
    ) throws IOException {

        if (inputApk == null ||
                !inputApk.exists()) {

            throw new IOException(
                    "Input APK for alignment does not exist."
            );
        }


        File parent =
                outputApk.getParentFile();

        if (parent != null &&
                !parent.exists()) {

            if (!parent.mkdirs() &&
                    !parent.exists()) {

                throw new IOException(
                        "Unable to create APK alignment directory."
                );
            }
        }


        try (
                ZipFile zipFile =
                        new ZipFile(inputApk);

                FileOutputStream fos =
                        new FileOutputStream(
                                outputApk
                        );

                BufferedOutputStream buffered =
                        new BufferedOutputStream(
                                fos
                        );

                CountingOutputStream counting =
                        new CountingOutputStream(
                                buffered
                        );

                ZipOutputStream zos =
                        new ZipOutputStream(
                                counting
                        )
        ) {

            byte[] buffer =
                    new byte[8192];


            Enumeration<? extends ZipEntry>
                    entries =
                    zipFile.entries();


            while (entries.hasMoreElements()) {

                ZipEntry original =
                        entries.nextElement();

                String name =
                        original.getName();


                /*
                 * Never preserve old signatures.
                 */
                if (name.startsWith(
                        "META-INF/"
                )) {

                    continue;
                }


                boolean resourcesArsc =
                        "resources.arsc".equals(
                                name
                        );


                ZipEntry entry =
                        new ZipEntry(name);


                if (original.getTime() != -1) {

                    entry.setTime(
                            original.getTime()
                    );
                }


                if (resourcesArsc) {

                    /*
                     * -------------------------------------------------
                     * resources.arsc
                     * -------------------------------------------------
                     *
                     * MUST be STORED.
                     */

                    entry.setMethod(
                            ZipEntry.STORED
                    );


                    entry.setSize(
                            original.getSize()
                    );


                    entry.setCompressedSize(
                            original.getSize()
                    );


                    entry.setCrc(
                            original.getCrc()
                    );


                    int filenameLength =
                            name.getBytes(
                                    StandardCharsets.UTF_8
                            ).length;


                    /*
                     * Position where the local header begins.
                     */
                    long localHeaderOffset =
                            counting.getCount();


                    /*
                     * Local ZIP header:
                     *
                     * 30 bytes
                     * + filename
                     * + extra field
                     *
                     * We want DATA to start
                     * on a 4-byte boundary.
                     */
                    int base =
                            (int) (
                                    localHeaderOffset
                                            + 30
                                            + filenameLength
                            );


                    /*
                     * Our extra field contains:
                     *
                     * 4 bytes header
                     * + padding bytes
                     *
                     * Therefore:
                     *
                     * dataOffset =
                     *
                     * base
                     * + 4
                     * + padding
                     */
                    int padding =
                            (4 - (
                                    (base + 4) % 4
                            )) % 4;


                    if (padding > 0) {

                        byte[] extra =
                                new byte[
                                        padding + 4
                                        ];


                        /*
                         * Extra field ID.
                         */
                        extra[0] =
                                (byte) 0xFF;

                        extra[1] =
                                (byte) 0xFF;


                        /*
                         * Extra field data length.
                         */
                        extra[2] =
                                (byte) padding;

                        extra[3] =
                                0;


                        entry.setExtra(
                                extra
                        );
                    }

                } else {

                    /*
                     * Preserve STORED entries.
                     *
                     * Other files can remain DEFLATED.
                     */
                    if (
                            original.getMethod()
                                    == ZipEntry.STORED
                    ) {

                        entry.setMethod(
                                ZipEntry.STORED
                        );


                        entry.setSize(
                                original.getSize()
                        );


                        entry.setCompressedSize(
                                original.getCompressedSize()
                        );


                        entry.setCrc(
                                original.getCrc()
                        );

                    } else {

                        entry.setMethod(
                                ZipEntry.DEFLATED
                        );
                    }
                }


                zos.putNextEntry(
                        entry
                );


                try (
                        BufferedInputStream input =
                                new BufferedInputStream(
                                        zipFile.getInputStream(
                                                original
                                        )
                                )
                ) {

                    int count;

                    while (
                            (count =
                                    input.read(buffer))
                                    != -1
                    ) {

                        zos.write(
                                buffer,
                                0,
                                count
                        );
                    }
                }


                zos.closeEntry();
            }


            zos.finish();
        }


        if (!outputApk.exists() ||
                outputApk.length() == 0) {

            throw new IOException(
                    "APK alignment failed."
            );
        }
    }


    /**
     * Tracks the exact number of bytes written
     * to the ZIP stream.
     */
    private static class CountingOutputStream
            extends OutputStream {

        private final OutputStream out;

        private long count = 0;


        CountingOutputStream(
                OutputStream out
        ) {

            this.out = out;
        }


        long getCount() {

            return count;
        }


        @Override
        public void write(
                int b
        ) throws IOException {

            out.write(b);

            count++;
        }


        @Override
        public void write(
                byte[] b
        ) throws IOException {

            out.write(b);

            count += b.length;
        }


        @Override
        public void write(
                byte[] b,
                int off,
                int len
        ) throws IOException {

            out.write(
                    b,
                    off,
                    len
            );

            count += len;
        }


        @Override
        public void flush()
                throws IOException {

            out.flush();
        }


        @Override
        public void close()
                throws IOException {

            out.close();
        }
    }
}