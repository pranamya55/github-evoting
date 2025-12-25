/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {Inject, Injectable} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {RouterStateSnapshot, TitleStrategy} from '@angular/router';
import {TranslateService} from '@ngx-translate/core';
import {Subscription} from 'rxjs';
import {APP_NAME} from '@sdm/shared-util-types';

@Injectable()
export class TranslatedTitleStrategy extends TitleStrategy {
  subscription?: Subscription;

  constructor(
    @Inject(APP_NAME) public readonly appName: string,
    private readonly title: Title,
    private readonly translate: TranslateService,
  ) {
    super();
  }

  override updateTitle(snapshot: RouterStateSnapshot) {
    if (this.subscription) this.subscription.unsubscribe();

    const titleKey = this.buildTitle(snapshot);
    if (titleKey) {
      this.subscription = this.translate.stream(titleKey).subscribe((title) => {
        this.title.setTitle(`${this.appName} | ${title}`);
      });
    } else {
      this.title.setTitle(this.appName);
    }
  }
}
