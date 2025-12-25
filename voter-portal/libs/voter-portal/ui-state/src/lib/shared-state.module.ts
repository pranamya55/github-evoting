/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import { BackendModule } from '@vp/voter-portal-data-access';
import { NavigationEffects } from './effects/navigation.effects';
import { SharedStateEffects } from './effects/shared-state.effects';
import * as fromSharedState from './reducer/shared-state.reducer';
import { SHARED_FEATURE_KEY } from '@vp/voter-portal-util-types';

@NgModule({
	imports: [
		CommonModule,
		StoreModule.forFeature(SHARED_FEATURE_KEY, fromSharedState.reducer),
		EffectsModule.forFeature([SharedStateEffects, NavigationEffects]),
		BackendModule,
	],
})
export class SharedStateModule {}
