import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'nas_model.dart';
import 'nas_repository.dart';

final nasListProvider = StateNotifierProvider<NasNotifier, AsyncValue<List<NasModel>>>((ref) {
  final repo = ref.watch(nasRepositoryProvider);
  return NasNotifier(repo);
});

class NasNotifier extends StateNotifier<AsyncValue<List<NasModel>>> {
  final NasRepository _repository;

  NasNotifier(this._repository) : super(const AsyncValue.loading()) {
    loadNasList();
  }

  Future<void> loadNasList() async {
    state = const AsyncValue.loading();
    try {
      final list = await _repository.getAllNas();
      state = AsyncValue.data(list);
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<void> createNas(NasModel nas) async {
    try {
      final created = await _repository.createNas(nas);
      state.whenData((list) {
        state = AsyncValue.data([...list, created]);
      });
    } catch (e) {
      rethrow;
    }
  }

  Future<void> updateNas(String id, NasModel nas) async {
    try {
      final updated = await _repository.updateNas(id, nas);
      state.whenData((list) {
        final index = list.indexWhere((item) => item.id == id);
        if (index >= 0) {
          final updatedList = List<NasModel>.from(list);
          updatedList[index] = updated;
          state = AsyncValue.data(updatedList);
        } else {
          state = AsyncValue.data([...list, updated]);
        }
      });
    } catch (e) {
      rethrow;
    }
  }

  Future<void> deleteNas(String id) async {
    try {
      await _repository.deleteNas(id);
      state.whenData((list) {
        state = AsyncValue.data(list.where((item) => item.id != id).toList());
      });
    } catch (e) {
      rethrow;
    }
  }
}
