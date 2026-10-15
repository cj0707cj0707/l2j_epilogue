package org.l2jmobius.gameserver.scripting.engine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject.Kind;

public class ScriptFileManager implements StandardJavaFileManager {
   private final StandardJavaFileManager _wrapped;
   private final List<ScriptClassData> _classOutputs = new ArrayList<>();

   public ScriptFileManager(StandardJavaFileManager wrapped) {
      this._wrapped = wrapped;
   }

   Iterable<ScriptClassData> getCompiledClasses() {
      return Collections.unmodifiableCollection(this._classOutputs);
   }

   @Override
   public int isSupportedOption(String option) {
      return this._wrapped.isSupportedOption(option);
   }

   @Override
   public ClassLoader getClassLoader(Location location) {
      return this._wrapped.getClassLoader(location);
   }

   @Override
   public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
      return this._wrapped.list(location, packageName, kinds, recurse);
   }

   @Override
   public String inferBinaryName(Location location, JavaFileObject file) {
      return this._wrapped.inferBinaryName(location, file);
   }

   @Override
   public boolean isSameFile(FileObject a, FileObject b) {
      return this._wrapped.isSameFile(a, b);
   }

   @Override
   public boolean handleOption(String current, Iterator<String> remaining) {
      return this._wrapped.handleOption(current, remaining);
   }

   @Override
   public boolean hasLocation(Location location) {
      return this._wrapped.hasLocation(location);
   }

   @Override
   public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
      return this._wrapped.getJavaFileForInput(location, className, kind);
   }

   @Override
   public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
      if (kind != Kind.CLASS) {
         return this._wrapped.getJavaFileForOutput(location, className, kind, sibling);
      } else {
         String javaName = className;
         if (className.contains("/")) {
            javaName = className.replace('/', '.');
         }

         ScriptClassData fileObject;
         if (sibling != null) {
            fileObject = new ScriptClassData(Paths.get(sibling.getName()), javaName, javaName.substring(javaName.lastIndexOf(46) + 1));
         } else {
            fileObject = new ScriptClassData(null, javaName, javaName.substring(javaName.lastIndexOf(46) + 1));
         }

         this._classOutputs.add(fileObject);
         return fileObject;
      }
   }

   @Override
   public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
      return this._wrapped.getFileForInput(location, packageName, relativeName);
   }

   @Override
   public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
      return this._wrapped.getFileForOutput(location, packageName, relativeName, sibling);
   }

   @Override
   public void flush() throws IOException {
      this._wrapped.flush();
   }

   @Override
   public void close() throws IOException {
      this._wrapped.close();
   }

   @Override
   public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files) {
      return this._wrapped.getJavaFileObjectsFromFiles(files);
   }

   @Override
   public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
      return this._wrapped.getJavaFileObjects(files);
   }

   @Override
   public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
      return this._wrapped.getJavaFileObjectsFromStrings(names);
   }

   @Override
   public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
      return this._wrapped.getJavaFileObjects(names);
   }

   @Override
   public void setLocation(Location location, Iterable<? extends File> path) throws IOException {
      this._wrapped.setLocation(location, path);
   }

   @Override
   public Iterable<? extends File> getLocation(Location location) {
      return this._wrapped.getLocation(location);
   }

   @Override
   public Location getLocationForModule(Location location, String moduleName) throws IOException {
      return this._wrapped.getLocationForModule(location, moduleName);
   }

   @Override
   public Location getLocationForModule(Location location, JavaFileObject fo) throws IOException {
      return this._wrapped.getLocationForModule(location, fo);
   }

   @Override
   public <S> ServiceLoader<S> getServiceLoader(Location location, Class<S> service) throws IOException {
      return this._wrapped.getServiceLoader(location, service);
   }

   @Override
   public String inferModuleName(Location location) throws IOException {
      return this._wrapped.inferModuleName(location);
   }

   @Override
   public Iterable<Set<Location>> listLocationsForModules(Location location) throws IOException {
      return this._wrapped.listLocationsForModules(location);
   }

   @Override
   public boolean contains(Location location, FileObject fo) throws IOException {
      return this._wrapped.contains(location, fo);
   }

   @Override
   public void setLocationFromPaths(Location location, Collection<? extends Path> paths) throws IOException {
      this._wrapped.setLocationFromPaths(location, paths);
   }

   @Override
   public void setLocationForModule(Location location, String moduleName, Collection<? extends Path> paths) throws IOException {
      this._wrapped.setLocationForModule(location, moduleName, paths);
   }

   @Override
   public Iterable<? extends Path> getLocationAsPaths(Location location) {
      return this._wrapped.getLocationAsPaths(location);
   }

   @Override
   public Path asPath(FileObject file) {
      return this._wrapped.asPath(file);
   }

   @Override
   public Iterable<? extends JavaFileObject> getJavaFileObjectsFromPaths(Collection<? extends Path> paths) {
      return this._wrapped.getJavaFileObjectsFromPaths(paths);
   }

   @Override
   public Iterable<? extends JavaFileObject> getJavaFileObjects(Path... paths) {
      return this._wrapped.getJavaFileObjects(paths);
   }
}
