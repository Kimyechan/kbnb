package com.buildup.kbnb.service.reservation;

import com.buildup.kbnb.advice.exception.ReservationException;
import com.buildup.kbnb.model.Reservation;
import com.buildup.kbnb.model.room.Room;
import com.buildup.kbnb.repository.reservation.ReservationRepository;
import com.buildup.kbnb.service.PaymentService;
import com.buildup.kbnb.util.payment.BootPayApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private BootPayApi bootPayApi;

    @Spy
    @InjectMocks
    private ReservationService reservationService;

    @Test
    @DisplayName("오늘 이전 날짜 예약 예외 발생")
    public void checkStrangeDatePrevious() {
        LocalDate now = LocalDate.now();

        assertThrows(ReservationException.class,
                () ->reservationService.checkStrangeDate(now.minusDays(2), now.minusDays(1)));
    }

    @Test
    @DisplayName("체크아웃이 체크인보다 느린 예약 예외 발생")
    public void checkStrangeDateCheckInCheckOutChange() {
        LocalDate now = LocalDate.now();

        assertThrows(ReservationException.class,
                () ->reservationService.checkStrangeDate(now.plusDays(2), now.plusDays(1)));
    }

    @Test
    @DisplayName("정상적인 날짜 예약시 예외 통과")
    public void checkStrangeDatePass() {
        LocalDate now = LocalDate.now();

        assertDoesNotThrow(() ->reservationService.checkStrangeDate(now.plusDays(1), now.plusDays(2)));
    }

    @Test
    @DisplayName("지난달 예약내역 조회")
    public void getPreviousMonth() {
        LocalDate now = LocalDate.now();
        LocalDate previous = now.minusMonths(1);

        LocalDate previousStartDate = previous.withDayOfMonth(1);
        LocalDate previousEndDate = now.withDayOfMonth(1).minusDays(1);

        given(reservationRepository.findByBetweenDateAndRoomId(1L, previousStartDate, previousEndDate))
                .willReturn(new ArrayList<>());

        reservationService.getBeforeMonthReservation(1L);

        verify(reservationRepository, times(1)).findByBetweenDateAndRoomId(1L, previousStartDate, previousEndDate);
    }

    @Test
    @DisplayName("지난 달 예약률 조회")
    public void getBeforeMonthReservationRate() {
        Long roomId = 1L;
        List<Reservation> reservations = getReservations();

        doReturn(reservations).when(reservationService).getBeforeMonthReservation(roomId);

        Double reservationRate = reservationService.getBeforeMonthReservationRate(roomId);

        assertThat(reservationRate).isEqualTo(1);
    }

    private List<Reservation> getReservations() {
        Room room = Room.builder()
                .name("test room")
                .build();

        LocalDate now = LocalDate.now();
        LocalDate previous = now.minusMonths(1);

        LocalDate previousStartDate = previous.withDayOfMonth(1);
        LocalDate previousEndDate = now.withDayOfMonth(1).minusDays(1);

        List<Reservation> reservations = new ArrayList<>();
        for (LocalDate date = previousStartDate; date.isBefore(previousEndDate); date = date.plusDays(1)) {
            Reservation reservation = Reservation.builder()
                    .checkIn(date)
                    .checkOut(date.plusDays(1))
                    .room(room)
                    .build();
            reservations.add(reservation);
        }
        return reservations;
    }

    @Test
    @DisplayName("예약률로 추천 숙소인지 확인")
    public void checkRecommendedRoom() {
        doReturn(0.9).when(reservationService).getBeforeMonthReservationRate(1L);

        Boolean isRecommendedRoom = reservationService.checkRecommendedRoom(1L);

        assertTrue(isRecommendedRoom);
    }
}