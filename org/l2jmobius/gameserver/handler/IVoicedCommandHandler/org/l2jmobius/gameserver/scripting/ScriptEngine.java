package org.l2jmobius.gameserver.scripting;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.scripting.engine.ScriptExecutor;
import org.w3c.dom.Document;

public class ScriptEngine implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(ScriptEngine.class.getName());
   public static final Path SCRIPT_FOLDER = ServerConfig.SCRIPT_ROOT.toPath();
   public static final Path MASTER_HANDLER_FILE = Paths.get(SCRIPT_FOLDER.toString(), "handlers", "MasterHandler.java");
   public static final Path EFFECT_MASTER_HANDLER_FILE = Paths.get(SCRIPT_FOLDER.toString(), "handlers", "EffectMasterHandler.java");
   private static final ScriptExecutor SCRIPT_EXECUTOR = new ScriptExecutor();
   private static final Set<String> EXCLUSIONS = new HashSet<>();

   protected ScriptEngine() {
      this.load();
   }

   @Override
   public void load() {
      EXCLUSIONS.clear();
      this.parseDatapackFile("config/Scripts.xml");
      LOGGER.info("Loaded " + EXCLUSIONS.size() + " files to exclude.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      try {
         final Map<String, Set<String>> excludePaths = new HashMap<>();
         this.forEach(document, "list", listNode -> this.forEach(listNode, "exclude", excludeNode -> {
            String excludeFile = this.parseString(excludeNode.getAttributes(), "file");
            excludePaths.putIfAbsent(excludeFile, new HashSet<>());
            this.forEach(excludeNode, "include", includeNode -> excludePaths.get(excludeFile).add(this.parseString(includeNode.getAttributes(), "file")));
         }));
         final int nameCount = SCRIPT_FOLDER.getNameCount();
         Files.walkFileTree(SCRIPT_FOLDER, new SimpleFileVisitor<Path>() {
            {
               Objects.requireNonNull(ScriptEngine.this);
            }

            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
               String fileName = file.getFileName().toString();
               if (!fileName.endsWith(".java")) {
                  return FileVisitResult.CONTINUE;
               } else {
                  List<String> relativePathParts = new ArrayList<>();
                  file.subpath(nameCount, file.getNameCount()).forEach(p -> relativePathParts.add(p.toString()));

                  for (int i = 0; i < relativePathParts.size(); i++) {
                     String currentPart = relativePathParts.get(i);
                     Set<String> includePath = excludePaths.get(currentPart);
                     if (includePath != null) {
                        boolean excludeScript = true;

                        for (int j = i + 1; j < relativePathParts.size(); j++) {
                           if (includePath.contains(relativePathParts.get(j))) {
                              excludeScript = false;
                              break;
                           }
                        }

                        if (excludeScript) {
                           ScriptEngine.EXCLUSIONS.add(file.toUri().getPath());
                           break;
                        }
                     }
                  }

                  return FileVisitResult.CONTINUE;
               }
            }
         });
      } catch (IOException var5) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Could not initialize. " + var5.getMessage());
      }
   }

   private void processDirectory(File dir, List<Path> files) {
      for (File file : dir.listFiles()) {
         if (file.isFile()) {
            String filePath = file.toURI().getPath();
            if (filePath.endsWith(".java") && !EXCLUSIONS.contains(filePath)) {
               files.add(file.toPath().toAbsolutePath());
            }
         } else if (file.isDirectory()) {
            this.processDirectory(file, files);
         }
      }
   }

   public void executeScript(Path sourceFiles) throws Exception {
      Path path = sourceFiles;
      if (!sourceFiles.isAbsolute()) {
         path = SCRIPT_FOLDER.resolve(sourceFiles);
      }

      path = path.toAbsolutePath();
      if (!Files.exists(path)) {
         throw new Exception("Script file does not exist: " + path.toString());
      } else {
         Entry<Path, Throwable> error = SCRIPT_EXECUTOR.executeScript(path);
         if (error != null) {
            Throwable cause = error.getValue();
            if (cause != null) {
               LOGGER.warning(TraceUtil.getStackTrace(cause));
            }

            throw new Exception("ScriptEngine: " + error.getKey() + " failed execution!", cause);
         }
      }
   }

   public void executeScriptList() throws Exception {
      if (!DevelopmentConfig.NO_QUESTS) {
         List<Path> files = new ArrayList<>();
         this.processDirectory(SCRIPT_FOLDER.toFile(), files);
         Map<Path, Throwable> invokationErrors = SCRIPT_EXECUTOR.executeScripts(files);

         for (Entry<Path, Throwable> entry : invokationErrors.entrySet()) {
            LOGGER.warning(this.getClass().getSimpleName() + ": " + entry.getKey() + " failed execution! " + entry.getValue().getMessage());
         }
      }
   }

   public Path getCurrentLoadingScript() {
      return SCRIPT_EXECUTOR.getCurrentExecutingScript();
   }

   public static ScriptEngine getInstance() {
      return ScriptEngine.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final ScriptEngine INSTANCE = new ScriptEngine();
   }
}
