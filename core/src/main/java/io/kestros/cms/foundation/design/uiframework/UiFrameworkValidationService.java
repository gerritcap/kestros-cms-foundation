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

package io.kestros.cms.foundation.design.uiframework;

import static io.kestros.cms.foundation.design.DesignConstants.THEME_PRIMARY_TYPE;
import static io.kestros.cms.foundation.utils.DesignUtils.getAllUiFrameworks;
import static io.kestros.commons.structuredslingmodels.validation.CommonValidators.hasDescription;
import static io.kestros.commons.structuredslingmodels.validation.CommonValidators.hasTitle;
import static io.kestros.commons.structuredslingmodels.validation.CommonValidators.modelListHasNoErrors;
import static io.kestros.commons.structuredslingmodels.validation.CommonValidators.modelListHasNoWarnings;
import static io.kestros.commons.structuredslingmodels.validation.ModelValidationMessageType.ERROR;
import static io.kestros.commons.structuredslingmodels.validation.ModelValidationMessageType.WARNING;

import io.kestros.cms.foundation.exceptions.InvalidThemeException;
import io.kestros.commons.structuredslingmodels.exceptions.ChildResourceNotFoundException;
import io.kestros.commons.structuredslingmodels.exceptions.ModelAdaptionException;
import io.kestros.commons.structuredslingmodels.validation.ModelValidationMessageType;
import io.kestros.commons.structuredslingmodels.validation.ModelValidator;
import io.kestros.commons.structuredslingmodels.validation.ModelValidatorBundle;
import io.kestros.commons.uilibraries.UiLibraryValidationService;

/**
 * Model Validation service for UiFrameworks.
 */
public class UiFrameworkValidationService extends UiLibraryValidationService {

  @Override
  public UiFramework getModel() {
    return (UiFramework) getGenericModel();
  }

  @Override
  public void registerBasicValidators() {
    addBasicValidator(hasTitle(getModel()));
    addBasicValidator(hasDescription(getModel(), WARNING));
    addBasicValidator(isAllIncludedScriptsFound());
    addBasicValidator(isAllDependenciesFound());

    addBasicValidator(hasFrameworkCode());
    addBasicValidator(isAllVendorLibrariesExist());
    addBasicValidator(hasValidDefaultTheme());
    addBasicValidator(isFrameworkCodeUnique());
    addBasicValidator(modelListHasNoWarnings(getModel().getThemes(), "Themes have no warnings."));
    addBasicValidator(modelListHasNoErrors(getModel().getThemes(), "Themes have no errors."));
  }

  ModelValidator hasFrameworkCode() {
    return new ModelValidator() {
      @Override
      public boolean isValid() {
        return !"common".equals(getModel().getFrameworkCode());
      }

      @Override
      public String getMessage() {
        return "UiFramework code is configured.";
      }

      @Override
      public ModelValidationMessageType getType() {
        return WARNING;
      }
    };
  }

  ModelValidator isAllVendorLibrariesExist() {
    return new ModelValidator() {
      @Override
      public boolean isValid() {
        return getModel().getVendorLibraries().size()
               == getModel().getIncludedVendorLibraryNames().size();
      }

      @Override
      public String getMessage() {
        return "All included vendor libraries exist and are valid.";
      }

      @Override
      public ModelValidationMessageType getType() {
        return ERROR;
      }
    };
  }

  ModelValidatorBundle hasValidDefaultTheme() {
    return new ModelValidatorBundle() {

      @Override
      public String getBundleMessage() {
        return "Has valid default Theme.";
      }

      @Override
      public void registerValidators() {
        addBasicValidator(hasDefaultThemeResource());
        addBasicValidator(isDefaultThemeValidResourceType());
      }

      @Override
      public boolean isAllMustBeTrue() {
        return true;
      }

      @Override
      public ModelValidationMessageType getType() {
        return ERROR;
      }
    };
  }

  ModelValidator hasDefaultThemeResource() {
    return new ModelValidator() {
      @Override
      public boolean isValid() {
        try {
          getModel().getDefaultTheme();
          return true;
        } catch (final ChildResourceNotFoundException exception) {
          return false;
        } catch (final InvalidThemeException e) {
          return true;
        }
      }

      @Override
      public String getMessage() {
        return "Has Default Theme resource.";
      }

      @Override
      public ModelValidationMessageType getType() {
        return ERROR;
      }
    };
  }

  ModelValidator isDefaultThemeValidResourceType() {
    return new ModelValidator() {
      @Override
      public boolean isValid() {
        try {
          getModel().getDefaultTheme();
          return true;
        } catch (final ModelAdaptionException exception) {
          return false;
        }
      }

      @Override
      public String getMessage() {
        return String.format("Default Theme has jcr:primaryType %s.", THEME_PRIMARY_TYPE);
      }

      @Override
      public ModelValidationMessageType getType() {
        return ERROR;
      }
    };
  }

  ModelValidator isFrameworkCodeUnique() {
    return new ModelValidator() {
      @Override
      public boolean isValid() {
        for (final UiFramework framework : getAllUiFrameworks(getModel().getResourceResolver(),
            true, false)) {
          if (!framework.getPath().equals(getModel().getPath()) && (framework.getName().equals(
              getModel().getName()) || framework.getFrameworkCode().equals(getModel().getName())
                                                                    || framework.getName().equals(
              getModel().getFrameworkCode()) || framework.getFrameworkCode().equals(
              getModel().getFrameworkCode()))) {
            return false;
          }
        }
        return true;
      }

      @Override
      public String getMessage() {
        return "Framework code and name are unique.";
      }

      @Override
      public ModelValidationMessageType getType() {
        return ERROR;
      }
    };
  }
}
