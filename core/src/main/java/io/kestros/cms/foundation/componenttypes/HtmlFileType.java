/*
 *      Copyright (C) 2020  Kestros, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.kestros.cms.foundation.componenttypes;

import io.kestros.commons.structuredslingmodels.filetypes.BaseFile;
import io.kestros.commons.structuredslingmodels.filetypes.FileType;
import java.util.Collections;
import java.util.List;

/**
 * FileType implementation for HTML files.
 */
public class HtmlFileType implements FileType {

  @Override
  public String getExtension() {
    return "html";
  }

  @Override
  public String getOutputContentType() {
    return "text/html";
  }

  @Override
  public List<String> getReadableContentTypes() {
    return Collections.singletonList("text/html");
  }

  @Override
  public String getName() {
    return "html";
  }

  @Override
  public <T extends BaseFile> Class<T> getFileModelClass() {
    return (Class<T>) HtmlFile.class;
  }


}
