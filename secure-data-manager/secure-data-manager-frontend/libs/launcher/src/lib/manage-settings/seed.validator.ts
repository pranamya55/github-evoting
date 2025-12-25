/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {AbstractControl, ValidationErrors, ValidatorFn} from '@angular/forms';

export class SeedValidator {
  static validate(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const seed = control.value;

      if (!seed) {
        return null;
      }

      const prefix = seed.substring(0, 2);
      const date = seed.substring(3, 11);
      const suffix = seed.substring(12);
      const isValidDelimiter = seed.charAt(2) == '_' && seed.charAt(11) == '_';

      const isValidPrefix = /^[A-Z]{2}$/.test(prefix);
      const isNumericDate = /^\d{8}$/.test(date);
      const isValidDate = SeedValidator.validateDate(date);
      const isValidSuffix = /^(?:TT|TP|PP)(0[1-9]|[1-9]\d)$/.test(suffix);

      const isSeedValid =
        isValidPrefix &&
        isNumericDate &&
        isValidDate &&
        isValidSuffix &&
        isValidDelimiter;

      return !isSeedValid
        ? {
            seed: {
              prefix: !isValidPrefix,
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
      day <= SeedValidator.daysInMonth(month, year)
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
