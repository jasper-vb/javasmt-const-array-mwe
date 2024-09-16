package com.example.javasmt_sigfpe_mwe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.visitors.FormulaVisitor;

class Main {
    private static void loadLibrary(String library) {
        if (library.equals("bitwuzlaj")) {
            String filename = System.mapLibraryName(library);
            int pos = filename.lastIndexOf('.');
            Path file;
            try {
                file = Files.createTempFile(filename.substring(0, pos), filename.substring(pos));
                file.toFile().deleteOnExit();
                try (var in = Main.class.getClassLoader().getResourceAsStream(filename);
                        var out = Files.newOutputStream(file)) {
                    in.transferTo(out);
                }
                System.load(file.toAbsolutePath().toString());
            } catch (IOException e) {
                throw new UnsatisfiedLinkError(e.getMessage());
            }
        } else {
            System.loadLibrary(library);
        }
    }

    public static SolverContext createSolverContext(Solvers solvers)
            throws InvalidConfigurationException {
        return SolverContextFactory.createSolverContext(Configuration.defaultConfiguration(),
                org.sosy_lab.common.log.LogManager.createNullLogManager(), ShutdownNotifier.createDummy(),
                solvers, Main::loadLibrary);
    }

    static class ArrayVisitor implements FormulaVisitor<Void> {
        @Override
        public Void visitFreeVariable(Formula f, String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Void visitBoundVariable(Formula f, int deBruijnIdx) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Void visitConstant(Formula f, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Void visitFunction(Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {
            System.out
                    .println("Function: " + functionDeclaration.getName() + ", kind: " + functionDeclaration.getKind());
            return null;
        }

        @Override
        public Void visitQuantifier(BooleanFormula f, Quantifier quantifier, List<Formula> boundVariables,
                BooleanFormula body) {
            throw new UnsupportedOperationException();
        }
    }

    public static void main(String[] args) throws InvalidConfigurationException, InterruptedException, SolverException {
        var ctx = createSolverContext(Solvers.BITWUZLA);
        var fmgr = ctx.getFormulaManager();
        var amgr = fmgr.getArrayFormulaManager();
        var prover = ctx.newProverEnvironment(ProverOptions.GENERATE_MODELS);

        var arr = amgr.makeArray("arr", FormulaType.getBitvectorTypeWithSize(32),
                FormulaType.getBitvectorTypeWithSize(8));

        System.out.println("Unsat: " + prover.isUnsat());
        var model = prover.getModel();

        fmgr.visit(model.eval(arr), new ArrayVisitor());

        System.out.println("x = " + model.eval(arr));
    }
}
