/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {inject, NgModule, provideAppInitializer} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';

import {AppComponent} from './app.component';
import {HttpClientModule} from "@angular/common/http";
import {FormsModule} from "@angular/forms";
import {PublicKeysSharingComponent} from "./public-keys-sharing/public-keys-sharing.component";
import {PublicKeysHashesComponent} from "./public-keys-hashes/public-keys-hashes.component";
import {KeystoresGeneration} from "./keystores-generation/keystores-generation.component";
import {ConfigurationService} from "./configuration/configuration.service";
import {lastValueFrom} from "rxjs";

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FormsModule,
    KeystoresGeneration,
    PublicKeysSharingComponent,
    PublicKeysHashesComponent
  ],
  providers: [
    provideAppInitializer(() => {
      const configService = inject(ConfigurationService);
      return lastValueFrom(configService.loadConfiguration());
    })
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}

export const API_BASE_PATH = 'http://localhost:8080/api/direct-trust'

export interface EvotingComponent {
  key: string,
  label: string,
  hasKey: boolean
}

export const EvotingComponents: { [char: string]: EvotingComponent } = {
  CANTON: {key: "CANTON", label: "Canton", hasKey: true},
  SDM_CONFIG: {key: "SDM_CONFIG", label: "SDM config", hasKey: true},
  SDM_TALLY: {key: "SDM_TALLY", label: "SDM tally", hasKey: true},
  VOTING_SERVER: {key: "VOTING_SERVER", label: "Voting Server", hasKey: true},
  CONTROL_COMPONENT_1: {key: "CONTROL_COMPONENT_1", label: "Control component 1", hasKey: true},
  CONTROL_COMPONENT_2: {key: "CONTROL_COMPONENT_2", label: "Control component 2", hasKey: true},
  CONTROL_COMPONENT_3: {key: "CONTROL_COMPONENT_3", label: "Control component 3", hasKey: true},
  CONTROL_COMPONENT_4: {key: "CONTROL_COMPONENT_4", label: "Control component 4", hasKey: true},
  PRINTING_COMPONENT: {key: "PRINTING_COMPONENT", label: "Printing component", hasKey: true},
  VERIFIER: {key: "VERIFIER", label: "Verifier", hasKey: false},
  DISPUTE_RESOLVER: {key: "DISPUTE_RESOLVER", label: "Dispute resolver", hasKey: true},
}

export enum Phase {
  KEYSTORES_GENERATION,
  PUBLIC_KEYS_SHARING,
  KEYSTORES_DOWNLOAD
}
