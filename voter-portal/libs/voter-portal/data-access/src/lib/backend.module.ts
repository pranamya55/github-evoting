/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
/*
 * To mock the backend, use the MockBackendService class
 * to provide the BackendService.
 */
import { NgModule } from '@angular/core';
import { BackendService } from './backend.service';
import { OvBackendService } from './ov-backend/ov-backend.service';

@NgModule({
	providers: [{ provide: BackendService, useClass: OvBackendService }],
})
export class BackendModule {}
