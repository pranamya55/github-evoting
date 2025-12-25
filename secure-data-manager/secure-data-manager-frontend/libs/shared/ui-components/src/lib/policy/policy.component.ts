/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, Input} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TranslateModule} from '@ngx-translate/core';

@Component({
  selector: 'sdm-policy',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './policy.component.html',
})
export class PolicyComponent {
  @Input({ required: true }) isMet!: boolean;
  @Input({ required: true }) label!: string;
}
