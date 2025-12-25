/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {computed, Injectable, signal} from "@angular/core";
import {API_BASE_PATH, EvotingComponents} from "../app.module";
import {HttpClient} from "@angular/common/http";
import {map, Observable, retry, tap} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class ConfigurationService {

  private readonly frontendConfigurationSignal = signal<FrontendConfigurationDto | undefined>(undefined);
  public readonly availableComponentSignal = computed(() => this.frontendConfigurationSignal()?.availableComponents
    .map(component => EvotingComponents[component])
    .sort((a, b) => a.label.localeCompare(b.label)) || []);
  public readonly availableStatesSignal = computed(() => this.frontendConfigurationSignal()?.availableStates
    .sort((a, b) => a.localeCompare(b)) || []);
  public readonly resetButtonModeSignal = computed(() => this.frontendConfigurationSignal()?.resetMode || ResetMode.NEVER_ENABLED);
  public readonly certificateDefaultValueSignal = computed(() => this.frontendConfigurationSignal()?.certificateDefaultValue);

  constructor(private readonly http: HttpClient) {
  }

  loadConfiguration(): Observable<FrontendConfigurationDto> {
    return this.http.get(`${API_BASE_PATH}/configuration`).pipe(
      retry({delay: 100}),
      map(value => <FrontendConfigurationDto>value),
      tap(fcd => fcd.availableStates = fcd.availableStates.sort((a, b) => a.localeCompare(b))),
      tap(fcd => fcd.availableComponents = fcd.availableComponents.sort((a, b) => a.localeCompare(b))),
      tap(fcd => this.frontendConfigurationSignal.set(fcd))
    );
  }
}

export interface CertificateDefaultValueDto {
  country: string;
  state: string;
  locality: string;
  organisation: string;
}

export interface FrontendConfigurationDto {
  certificateDefaultValue: CertificateDefaultValueDto;
  availableStates: string[];
  availableComponents: string[];
  resetMode: ResetMode;
}

export enum ResetMode {
  ALWAYS_ENABLED = 'ALWAYS_ENABLED',
  ENABLED_AT_END = 'ENABLED_AT_END',
  NEVER_ENABLED = 'NEVER_ENABLED'
}
