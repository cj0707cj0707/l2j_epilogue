package org.l2jmobius.gameserver.scripting.engine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.l2jmobius.gameserver.scripting.annotations.Disabled;

public class ScriptExecutor {
   private static final Logger LOGGER = Logger.getLogger(ScriptExecutor.class.getName());
   private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();
   private static final ScriptClassLoader SCRIPT_CLASS_LOADER = new ScriptClassLoader(ClassLoader.getSystemClassLoader());
   private static final List<String> OPTIONS = new ArrayList<>();
   private static Path _currentExecutingScript;

   public ScriptExecutor() {
      this.addOptionIfNotNull(OPTIONS, "data/scripts", "-sourcepath");
      this.addOptionIfNotNull(OPTIONS, "source,lines,vars", "-g:");
   }

   private boolean addOptionIfNotNull(List<String> list, String nullChecked, String before) {
      if (nullChecked == null) {
         return false;
      } else {
         if (before.endsWith(":")) {
            list.add(before + nullChecked);
         } else {
            list.add(before);
            list.add(nullChecked);
         }

         return true;
      }
   }

   public Map<Path, Throwable> executeScripts(Iterable<Path> sourcePaths) throws Exception {
      DiagnosticCollector<JavaFileObject> fileManagerDiagnostics = new DiagnosticCollector<>();
      DiagnosticCollector<JavaFileObject> compilationDiagnostics = new DiagnosticCollector<>();

      Object var30;
      try (ScriptFileManager fileManager = new ScriptFileManager(COMPILER.getStandardFileManager(fileManagerDiagnostics, null, StandardCharsets.UTF_8))) {
         List<String> sourcePathStrings = new ArrayList<>();

         for (Path sourcePath : sourcePaths) {
            sourcePathStrings.add(sourcePath.toAbsolutePath().toString());
         }

         StringWriter strOut = new StringWriter();
         PrintWriter out = new PrintWriter(strOut);
         boolean compilationSuccess = COMPILER.getTask(
               out, fileManager, compilationDiagnostics, OPTIONS, null, fileManager.getJavaFileObjectsFromStrings(sourcePathStrings)
            )
            .call();
         if (!compilationSuccess) {
            this.logDiagnostics(out, fileManagerDiagnostics, compilationDiagnostics);
            throw new RuntimeException(strOut.toString());
         }

         Map<Path, Throwable> executionFailures = new HashMap<>();
         Iterable<ScriptClassData> compiledClasses = fileManager.getCompiledClasses();
         SCRIPT_CLASS_LOADER.addCompiledClasses(compiledClasses);

         for (Path sourcePath : sourcePaths) {
            boolean found = false;

            for (ScriptClassData compiledClass : compiledClasses) {
               Path compiledSourcePath = compiledClass.getSourcePath();
               if (compiledSourcePath != null && (compiledSourcePath.equals(sourcePath) || compiledSourcePath.endsWith(sourcePath))) {
                  String javaName = compiledClass.getJavaName();
                  if (javaName.indexOf(36) == -1) {
                     found = true;
                     _currentExecutingScript = compiledSourcePath;

                     try {
                        try {
                           Class<?> javaClass = SCRIPT_CLASS_LOADER.loadClass(javaName);
                           this.executeMainMethod(javaClass, compiledSourcePath);
                        } catch (Exception var25) {
                           executionFailures.put(compiledSourcePath, var25);
                        }
                        break;
                     } finally {
                        _currentExecutingScript = null;
                     }
                  }
               }
            }

            if (!found) {
               LOGGER.severe("Compilation successful, but class corresponding to " + sourcePath.toString() + " not found!");
            }
         }

         var30 = executionFailures;
      }

      return (Map<Path, Throwable>)var30;
   }

   private void logDiagnostics(
      PrintWriter out, DiagnosticCollector<JavaFileObject> fileManagerDiagnostics, DiagnosticCollector<JavaFileObject> compilationDiagnostics
   ) {
      out.println();
      out.println("----------------");
      out.println("File diagnostics");
      out.println("----------------");

      for (Diagnostic<? extends JavaFileObject> diagnostic : fileManagerDiagnostics.getDiagnostics()) {
         this.logDiagnostic(out, diagnostic);
      }

      out.println();
      out.println("-----------------------");
      out.println("Compilation diagnostics");
      out.println("-----------------------");

      for (Diagnostic<? extends JavaFileObject> diagnostic : compilationDiagnostics.getDiagnostics()) {
         this.logDiagnostic(out, diagnostic);
      }
   }

   private void logDiagnostic(PrintWriter out, Diagnostic<? extends JavaFileObject> diagnostic) {
      String sourceName = diagnostic.getSource() != null ? diagnostic.getSource().getName() : "Unknown Source";
      out.println("\t" + diagnostic.getKind() + ": " + sourceName + ", Line " + diagnostic.getLineNumber() + ", Column " + diagnostic.getColumnNumber());
      out.println("\t\tcode: " + diagnostic.getCode());
      out.println("\t\tmessage: " + diagnostic.getMessage(null));
   }

   private void executeMainMethod(Class<?> javaClass, Path compiledSourcePath) throws Exception {
      if (!javaClass.isAnnotationPresent(Disabled.class)) {
         for (Method method : javaClass.getMethods()) {
            if (method.getName().equals("main")
               && Modifier.isStatic(method.getModifiers())
               && method.getParameterCount() == 1
               && method.getParameterTypes()[0] == String[].class) {
               method.invoke(null, new String[]{compiledSourcePath.toString()});
               break;
            }
         }
      }
   }

   public Entry<Path, Throwable> executeScript(Path sourcePath) throws Exception {
      Map<Path, Throwable> executionFailures = this.executeScripts(Arrays.asList(sourcePath));
      return !executionFailures.isEmpty() ? executionFailures.entrySet().iterator().next() : null;
   }

   public Path getCurrentExecutingScript() {
      return _currentExecutingScript;
   }
}
