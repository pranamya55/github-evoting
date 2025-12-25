/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {SessionService} from "../session/session.service";
import {API_BASE_PATH, EvotingComponents} from "../app.module";
import {switchMap} from "rxjs";
import {HttpClient} from "@angular/common/http";

@Component({
  selector: 'app-public-keys-sharing',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './public-keys-sharing.component.html',
  styleUrl: './public-keys-sharing.component.css'
})
export class PublicKeysSharingComponent {

  importForm: FormGroup;
  importInProgress: boolean = false;
  error: boolean = false;
  selectedFiles: FileList | undefined;
  wantedNumberOfKey: number | undefined;

  constructor(private fb: FormBuilder, public phase: SessionService, private http: HttpClient) {
    this.importForm = this.fb.group({
      // eslint-disable-next-line @typescript-eslint/unbound-method
      publicKeyLocation: ['', [Validators.required]]
    });
    this.wantedNumberOfKey = Object.keys(EvotingComponents).length - 1;
  }

  selectFiles(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.selectedFiles = target.files as FileList;
  }

  downloadPublicKeys() {
    this.phase.getSession().pipe(
      switchMap(sessionId => this.http.get<CertificatesDownloadDto>(`${API_BASE_PATH}/public-keys/${sessionId}`, {responseType: "json"}))
    ).subscribe(archive => {
      const src = `data:text/csv;base64,${archive.content}`;
      const link = document.createElement("a");
      link.href = src;
      link.download = archive.filename;
      link.click();
      link.remove();
    });
  }

  importPublicKeys() {
    if (this.selectedFiles) {
      const formData: FormData = new FormData();
      for (let i = 0; i < this.selectedFiles.length; i++) {
        const file: File = this.selectedFiles[i];
        formData.append(file.name, file);
      }
      this.importInProgress = true;
      this.phase.getSession().pipe(
        switchMap(sessionId => this.http.post(`${API_BASE_PATH}/public-keys/${sessionId}`, formData))
      ).subscribe({
        next: () => {
          this.importInProgress = false;
          this.importForm.reset();
          this.phase.update()
        },
        error: () => {
          this.importInProgress = false;
          this.importForm.reset();
          this.error = true;
        }
      });
    }
  }
}

interface CertificatesDownloadDto {
  filename: string;
  content: string;
}
