/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Route } from '@angular/router';
import {ContentPageComponent} from "./content-page/content-page.component";

export const appRoutes: Route[] = [
	{
		path: '',
		pathMatch: 'full',
		component: ContentPageComponent
	},
];
