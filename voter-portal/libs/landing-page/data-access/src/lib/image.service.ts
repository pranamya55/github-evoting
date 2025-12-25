/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {HttpClient} from "@angular/common/http";
import {inject, Injectable} from '@angular/core';
import {from, Observable, switchMap} from "rxjs";

@Injectable({
  providedIn: 'root',
})
export class ImageService {
  private readonly http = inject(HttpClient);

  loadBase64(url: string): Observable<string> {
    return this.http.get(url, { responseType: 'blob' }).pipe(
        switchMap(blob => from(this.convertBlobToBase64(blob)))
    );
  }

  private convertBlobToBase64(blob: Blob): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onerror = reject;
      reader.onload = () => {
        const dataUrl = reader.result as string;
        const base64 = dataUrl.split(',')[1]; // remove prefix
        resolve(base64);
      };
      reader.readAsDataURL(blob);
    });
  }

}
