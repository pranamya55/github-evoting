/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Metric} from '@sdm/shared-util-types';
import {forkJoin, map, Observable} from 'rxjs';
import {environment} from "@sdm/shared-ui-config";

@Injectable({
	providedIn: 'root',
})
export class MetricService {
	constructor(private readonly http: HttpClient) {
	}

	getProcessorName(): Observable<string> {
		return this.http
			.get<Metric>(`${environment.backendPath}/sdm-shared/metrics/processorName`)
			.pipe(map((metric) => metric.name));
	}

	getUsedCpuPercentage(): Observable<number> {
		return this.http.get<Metric>(`${environment.backendPath}/actuator/metrics/process.cpu.usage`).pipe(
			map((metric) => {
				const cpuPercentage = this.getFirstMeasurement(metric) * 100;
				return parseFloat(cpuPercentage.toFixed(2));
			}),
		);
	}

	getUsedMemoryPercentage(): Observable<number> {
		return forkJoin({
			usedMemory: this.getUsedMemory(),
			maxMemory: this.getMaxMemory(),
		}).pipe(
			map(({usedMemory, maxMemory}) => {
				if (maxMemory === 0) return 0;

				const memoryPercentage = (usedMemory / maxMemory) * 100;
				return Math.round(memoryPercentage);
			}),
		);
	}

	private getUsedMemory(): Observable<number> {
		return this.http
			.get<Metric>(`${environment.backendPath}/actuator/metrics/jvm.memory.used`)
			.pipe(map((metric) => this.getFirstMeasurement(metric)));
	}

	private getMaxMemory(): Observable<number> {
		return this.http
			.get<Metric>(`${environment.backendPath}/actuator/metrics/jvm.memory.max`)
			.pipe(map((metric) => this.getFirstMeasurement(metric)));
	}

	private getFirstMeasurement(metric: Metric) {
		return metric.measurements[0].value;
	}
}
