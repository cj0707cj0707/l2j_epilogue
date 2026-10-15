package org.l2jmobius.gameserver.scripting.engine;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

public class ScriptClassData implements JavaFileObject {
   private final Path _sourcePath;
   private final String _javaName;
   private final String _javaSimpleName;
   private final ByteArrayOutputStream _out;

   public ScriptClassData(Path sourcePath, String javaName, String javaSimpleName) {
      this._sourcePath = sourcePath;
      this._javaName = javaName;
      this._javaSimpleName = javaSimpleName;
      this._out = new ByteArrayOutputStream();
   }

   public Path getSourcePath() {
      return this._sourcePath;
   }

   public String getJavaName() {
      return this._javaName;
   }

   public String getJavaSimpleName() {
      return this._javaSimpleName;
   }

   public byte[] getJavaData() {
      return this._out.toByteArray();
   }

   @Override
   public URI toUri() {
      return null;
   }

   @Override
   public String getName() {
      return null;
   }

   @Override
   public InputStream openInputStream() {
      return null;
   }

   @Override
   public OutputStream openOutputStream() {
      return this._out;
   }

   @Override
   public Reader openReader(boolean ignoreEncodingErrors) {
      return null;
   }

   @Override
   public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return null;
   }

   @Override
   public Writer openWriter() {
      return null;
   }

   @Override
   public long getLastModified() {
      return 0L;
   }

   @Override
   public boolean delete() {
      return false;
   }

   @Override
   public Kind getKind() {
      return Kind.CLASS;
   }

   @Override
   public boolean isNameCompatible(String simpleName, Kind kind) {
      return kind == Kind.CLASS && this._javaSimpleName.contentEquals(simpleName);
   }

   @Override
   public NestingKind getNestingKind() {
      return NestingKind.TOP_LEVEL;
   }

   @Override
   public Modifier getAccessLevel() {
      return null;
   }
}
