/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {AbstractControl, ValidationErrors, ValidatorFn} from '@angular/forms';

export class OrganisationValidator {
  static validate(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const organisation = control.value;

      if (!organisation) {
        return null;
      }

      const prefix = organisation.substring(0, 2);
      const contentPrefix = organisation.substring(3, 5);
      const date = organisation.substring(6, 14);
      const suffix = organisation.substring(15);
      const isValidDelimiter = organisation.charAt(2) == '_' && organisation.charAt(5) == '_' && organisation.charAt(14) == '_';

      const isValidPrefix = /^DT$/.test(prefix);
      const isValidContentPrefix = /^[A-Z]{2}$/.test(contentPrefix);
      const isNumericDate = /^\d{8}$/.test(date);
      const isValidDate = OrganisationValidator.validateDate(date);
      const isValidSuffix = /^(?:TT|TP|PP)(0[1-9]|[1-9]\d)$/.test(suffix);

      const isOrganisationValid =
        isValidPrefix &&
        isValidContentPrefix &&
        isNumericDate &&
        isValidDate &&
        isValidSuffix &&
        isValidDelimiter;

      return !isOrganisationValid
        ? {
          organisation: {
            prefix: !isValidPrefix,
            contentPrefix: !isValidContentPrefix,
            date: !isNumericDate || !isValidDate,
            suffix: !isValidSuffix,
            delimiter: !isValidDelimiter,
          },
        }
        : null;
    };
  }

  private static validateDate(date: string) {
    const year = parseInt(date.substring(0, 4), 10);
    const month = parseInt(date.substring(4, 6), 10);
    const day = parseInt(date.substring(6), 10);

    return (
      month > 0 &&
      month <= 12 &&
      day > 0 &&
      day <= OrganisationValidator.daysInMonth(month, year)
    );
  }

  private static daysInMonth(month: number, year: number): number {
    switch (month) {
      case 2:
        return (year % 4 == 0 && year % 100) || year % 400 == 0 ? 29 : 28;
      case 4:
      case 6:
      case 9:
      case 11:
        return 30;
      default:
        return 31;
    }
  }
}
