package org.tbc.content;

import org.tbc.content.compile.ContentCompiler;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CLI: {@code compile --content DIR --base-dbc DIR --out DIR [--mpq-name NAME]}
 */
public final class ContentMain {
    private ContentMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || "help".equals(args[0]) || "-h".equals(args[0])) {
            printHelp();
            return;
        }
        if (!"compile".equals(args[0])) {
            System.err.println("unknown command: " + args[0]);
            printHelp();
            System.exit(2);
        }
        Path content = null;
        Path baseDbc = null;
        Path out = null;
        String mpqName = "patch-tbc-custom.MPQ";
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--content" -> content = Path.of(requireArg(args, ++i, "--content"));
                case "--base-dbc" -> baseDbc = Path.of(requireArg(args, ++i, "--base-dbc"));
                case "--out" -> out = Path.of(requireArg(args, ++i, "--out"));
                case "--mpq-name" -> mpqName = requireArg(args, ++i, "--mpq-name");
                default -> throw new IllegalArgumentException("unknown arg: " + args[i]);
            }
        }
        if (content == null || baseDbc == null || out == null) {
            System.err.println("compile requires --content, --base-dbc, and --out");
            printHelp();
            System.exit(2);
        }
        if (!Files.isDirectory(baseDbc)) {
            System.err.println("base DBC dir missing: " + baseDbc.toAbsolutePath());
            System.err.println("Set CONTENT_BASE_DBC or pass --base-dbc to DataDir/dbc");
            System.exit(1);
        }
        var result = new ContentCompiler().compile(content, baseDbc, out, mpqName);
        System.out.println("content compile ok: deltas=" + result.deltas()
                + " sql=" + result.sqlCount()
                + " dbc=" + result.dbcFiles().size());
        System.out.println("  sql: " + result.sqlFile().toAbsolutePath());
        System.out.println("  mpq: " + result.mpqFile().toAbsolutePath());
        System.out.println("build.bat copies this into WoW-2.4.3-client/Data/ automatically.");
    }

    private static String requireArg(String[] args, int i, String flag) {
        if (i >= args.length) {
            throw new IllegalArgumentException(flag + " needs a value");
        }
        return args[i];
    }

    private static void printHelp() {
        System.out.println("""
                tbc-content — YAML content deltas → SQL + DBC + patch MPQ
                
                Usage:
                  compile --content <dir> --base-dbc <DataDir/dbc> --out <dir> [--mpq-name patch-tbc-custom.MPQ]
                
                Author YAML under content/spells/ (etc.). Bindings: content/bindings/tbc243/
                Never overwrites stock client MPQs; emits a new overlay archive only.
                """);
    }
}
