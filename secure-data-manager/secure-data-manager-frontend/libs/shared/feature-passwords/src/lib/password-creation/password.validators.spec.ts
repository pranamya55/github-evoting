/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {PasswordValidators} from './password.validators';
import {FormControl, FormGroup} from '@angular/forms';

describe('PasswordValidators', () => {
  describe('validate', () => {
    it('should return null if the value is falsy', () => {
      expect(PasswordValidators.validate(new FormControl())).toBeNull();
    });

    it('should return null if the value is correct', () => {
      expect(
        PasswordValidators.validate(
          new FormControl('aaaaaaBBBBBB111111%%%%%%'),
        ),
      ).toBeNull();
    });

    it('should return a validation error if the value is to short', () => {
      expect(
        PasswordValidators.validate(new FormControl('aaaaaaBBBBBB111111%%%%%')),
      ).toEqual(
        expect.objectContaining({
          length: true,
        }),
      );
    });

    it('should return a validation error if the value is to long', () => {
      expect(
        PasswordValidators.validate(
          new FormControl(
            'aaaaaaBBBBBB111111%%%%%%_________________________________________',
          ),
        ),
      ).toEqual(
        expect.objectContaining({
          length: true,
        }),
      );
    });

    it('should return a validation error if the value does not contain digits', () => {
      expect(
        PasswordValidators.validate(
          new FormControl('aaaaaaBBBBBBcccccc%%%%%%'),
        ),
      ).toEqual(
        expect.objectContaining({
          digit: true,
        }),
      );
    });

    it('should return a validation error if the value does not contain special character', () => {
      expect(
        PasswordValidators.validate(
          new FormControl('aaaaaaBBBBBB111111222222'),
        ),
      ).toEqual(
        expect.objectContaining({
          specialChar: true,
        }),
      );
    });

    it('should return a validation error if the value does not contain lower case character', () => {
      expect(
        PasswordValidators.validate(
          new FormControl('AAAAAABBBBBB111111%%%%%%'),
        ),
      ).toEqual(
        expect.objectContaining({
          lowerCaseChar: true,
        }),
      );
    });

    it('should return a validation error if the value does not contain upper case character', () => {
      expect(
        PasswordValidators.validate(
          new FormControl('aaaaaabbbbbb111111%%%%%%'),
        ),
      ).toEqual(
        expect.objectContaining({
          uppercaseChar: true,
        }),
      );
    });
  });

  describe('confirm', () => {
    it('should throw when used on anything else than a form group', () => {
      expect(() => PasswordValidators.confirm(new FormControl())).toThrow();
    });

    it('should throw when used on a form group that does not have both a "password" and a "confirmation" field', () => {
      expect(() => PasswordValidators.confirm(new FormGroup({}))).toThrow();
    });

    it('should return null if the "password" and a "confirmation" field have the same value', () => {
      const formGroup = new FormGroup({
        password: new FormControl('Mock password'),
        confirmation: new FormControl('Mock password'),
      });

      expect(PasswordValidators.confirm(formGroup)).toBeNull();
    });

    it('should return a validation error if the "password" and a "confirmation" field do not have the same value', () => {
      const formGroup = new FormGroup({
        password: new FormControl('Mock password'),
        confirmation: new FormControl('Another mock password'),
      });

      expect(PasswordValidators.confirm(formGroup)).toEqual({
        confirmation: true,
      });
    });
  });
});
