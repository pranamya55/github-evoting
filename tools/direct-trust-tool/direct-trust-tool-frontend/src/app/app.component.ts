/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component} from '@angular/core';
import {SessionService} from "./session/session.service";
import {Phase} from "./app.module";
import {ConfigurationService, ResetMode} from "./configuration/configuration.service";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  standalone: false
})
export class AppComponent {

  protected readonly Phase = Phase;

  constructor(protected session: SessionService, private readonly configuration: ConfigurationService) {
  }

  get resetDataDisable() {
    switch (this.configuration.resetButtonModeSignal()) {
      case ResetMode.ALWAYS_ENABLED:
        return false;
      case ResetMode.NEVER_ENABLED:
        return true;
      case ResetMode.ENABLED_AT_END:
        return this.session.currentPhase !== Phase.KEYSTORES_DOWNLOAD;
      default:
        return false;
    }
  }

  get keystoreGenerationState() {
    return this.session.currentPhase === Phase.KEYSTORES_GENERATION ? ActivationState.selected : ActivationState.disabled;
  }

  get publicKeySharingState() {
    return (this.session.currentPhase === Phase.PUBLIC_KEYS_SHARING) || (this.session.currentPhase === Phase.KEYSTORES_DOWNLOAD) ? ActivationState.selected : ActivationState.disabled;
  }

  resetData() {
    if (confirm("Are you sure that you want to reset the data? All data will be lost.")) {
      this.session.reset();
    }
  }
}


enum ActivationState {
  selected = 'active',
  disabled = 'disabled'
}
