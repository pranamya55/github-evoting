/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SessionService} from '../session/session.service';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {API_BASE_PATH, EvotingComponent, EvotingComponents} from '../app.module';
import {HttpClient} from '@angular/common/http';
import {switchMap} from 'rxjs';
import {ConfigurationService} from "../configuration/configuration.service";

@Component({
  selector: 'app-public-keys-hashes',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './public-keys-hashes.component.html',
  styleUrl: './public-keys-hashes.component.css'
})
export class PublicKeysHashesComponent {

  downloadForm: FormGroup;
  private fingerprints!: { [component: string]: string };

  constructor(private readonly fb: FormBuilder, private readonly http: HttpClient, public session: SessionService, public configuration: ConfigurationService) {

    this.downloadForm = this.fb.group({
      customSuffix: ['', [Validators.pattern(/^\w*$/)]],
    });

    this.session.getSession().pipe(
      switchMap(sessionId => this.http.get(`${API_BASE_PATH}/public-keys/fingerprints/${sessionId}`, {responseType: "json"}))
    ).subscribe(value => {
      this.fingerprints = <{ [component: string]: string }>value;
    });
  }

  downloadKeystores() {
    this.session.getSession().pipe(
      switchMap(sessionId => this.http.get<KeystoresDownloadDto>(`${API_BASE_PATH}/key-store-download/${sessionId}`, {responseType: "json"})
      )
    ).subscribe(archive => {
      const src = `data:text/csv;base64,${archive.content}`;
      const link = document.createElement("a");
      link.href = src;
      link.download = archive.filename;
      link.click();
      link.remove();
    });
  }

  private get availableComponents(): EvotingComponent[] {
    return this.configuration.availableComponentSignal();
  }

  get hasComponents() {
    return this.availableComponents.length > 0;
  }

  print() {
    const printContent = document.getElementById("fingerprints-table");
    if (!printContent) {
      return;
    }
    const WindowPrt = window.open('', '', 'left=0,top=0,width=900,height=900,toolbar=0,scrollbars=0,status=0');
    if (!WindowPrt) {
      return;
    }

    WindowPrt.document.write('<link rel="stylesheet" type="text/css" href="public-keys-hashes.component.css">');

    WindowPrt.document.write(printContent.innerHTML);
    WindowPrt.document.close();
    WindowPrt.focus();
    WindowPrt.print();
    WindowPrt.close();
  }

  getFingerprints(): string[][] {
    if (!this.fingerprints) {
      return [['-', '']];
    }
    return Object.entries(this.fingerprints)
      .map(value => [EvotingComponents[value[0].toUpperCase()].label, value[1]])
      .sort();
  }
}

interface KeystoresDownloadDto {
  filename: string;
  content: string;
}
