package com.deshaion.transfertest.service;

import com.deshaion.transfertest.model.Transfer;
import io.reactivex.Completable;

public interface TransferService {
    Completable createTransfer(Transfer transfer);
}
