import SwiftUI

@main
struct MahjongABCApp: App {
  @StateObject private var viewModel = GameViewModel()
  @Environment(\.scenePhase) private var scenePhase

  var body: some Scene {
    WindowGroup {
      RootView(viewModel: viewModel)
        .preferredColorScheme(.dark)
        .onChange(of: scenePhase) { phase in
          viewModel.audio.setMusicActive(phase == .active)
        }
    }
  }
}

struct RootView: View {
  @ObservedObject var viewModel: GameViewModel

  var body: some View {
    ZStack {
      RadialGradient(
        colors: [Color(hex: 0x137052), Color(hex: 0x0C543D), Color(hex: 0x053324)],
        center: .center,
        startRadius: 20,
        endRadius: 520
      )
      .ignoresSafeArea()

      switch viewModel.screen {
      case .home:
        HomeView(vm: viewModel)
      case .levels(let diff):
        LevelMapView(vm: viewModel, initialDifficulty: diff)
      case .game:
        GameBoardView(vm: viewModel)
      case .settings:
        SettingsView(vm: viewModel)
      }
    }
  }
}

struct HomeView: View {
  @ObservedObject var vm: GameViewModel

  var body: some View {
    ScrollView {
      VStack(spacing: 16) {
        HStack {
          Text("★ \(vm.totalScore) pts")
            .font(.subheadline.bold())
            .foregroundColor(Color(hex: 0xFFFBF0))
            .padding(.horizontal, 14).padding(.vertical, 8)
            .background(Color(hex: 0x0E4735))
            .clipShape(Capsule())
            .overlay(Capsule().stroke(Color(hex: 0x2B8C6B), lineWidth: 1))
          Spacer()
          WoodCircleBtn(icon: vm.musicEnabled ? "speaker.wave.2.fill" : "speaker.slash.fill") {
            vm.musicEnabled.toggle()
          }
          WoodCircleBtn(icon: "gearshape.fill") { vm.push(.settings) }
        }

        HStack(spacing: -6) {
          MiniTile(glyph: "A", color: Color(hex: 0xD63638), angle: -8)
          MiniTile(glyph: "1", color: Color(hex: 0x1958C9), angle: 0).offset(y: -6)
          MiniTile(glyph: "أ", color: Color(hex: 0xB55309), angle: 8)
        }.padding(.top, 6)

        Text("Mahjong ABC")
          .font(.system(size: 34, weight: .bold, design: .rounded))
          .foregroundColor(Color(hex: 0xFFFBF0))
        Text("Apprends les lettres A-Z, chiffres 0-10 et الحروف أ-ي")
          .font(.subheadline)
          .foregroundColor(Color(hex: 0xB9DFCE))
          .multilineTextAlignment(.center)

        ScrollView(.horizontal, showsIndicators: false) {
          HStack(spacing: 8) {
            ForEach(TilePack.allCases) { pack in
              let sel = vm.selectedPack == pack
              Button(pack.titleFr) { vm.selectedPack = pack }
                .font(.subheadline.bold())
                .padding(.horizontal, 14).padding(.vertical, 8)
                .background(sel ? Color(hex: 0xFFCA3A) : Color(hex: 0x0E4735))
                .foregroundColor(sel ? Color(hex: 0x053324) : Color(hex: 0xB9DFCE))
                .cornerRadius(12)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: 0x2B8C6B), lineWidth: 1))
            }
          }
        }

        VStack(alignment: .leading, spacing: 12) {
          HStack {
            VStack(alignment: .leading) {
              Text("Niveaux Infinis").font(.title3.bold()).foregroundColor(Color(hex: 0xFFFBF0))
              Text("Facile, Moyen, Difficile • ∞").font(.caption).foregroundColor(Color(hex: 0xB9DFCE))
            }
            Spacer()
            Text("Niv. \(vm.unlockedLevel(for: vm.selectedDifficulty)) / ∞")
              .font(.caption.bold()).foregroundColor(Color(hex: 0xFFCA3A))
              .padding(.horizontal, 10).padding(.vertical, 6)
              .background(Color(hex: 0xFFCA3A).opacity(0.16)).cornerRadius(10)
          }
          HStack(spacing: 8) {
            ForEach(Difficulty.allCases) { diff in
              let sel = vm.selectedDifficulty == diff
              Button {
                vm.selectedDifficulty = diff
              } label: {
                VStack {
                  Text(diff.labelFr).font(.subheadline.bold())
                  Text(diff.labelAr).font(.caption2)
                }
                .frame(maxWidth: .infinity).padding(.vertical, 10)
                .background(sel ? Color(hex: 0xD97736) : Color(hex: 0x053324).opacity(0.6))
                .foregroundColor(Color(hex: 0xFFFBF0))
                .cornerRadius(14)
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(sel ? Color(hex: 0xFFCA3A) : Color(hex: 0x2B8C6B), lineWidth: sel ? 2 : 1))
              }
            }
          }
          HStack(spacing: 10) {
            Button("▶ Niveau \(vm.unlockedLevel(for: vm.selectedDifficulty))") {
              vm.startCampaign(difficulty: vm.selectedDifficulty, level: vm.unlockedLevel(for: vm.selectedDifficulty), pack: vm.selectedPack)
            }
            .frame(maxWidth: .infinity, minHeight: 50)
            .font(.headline.bold())
            .background(Color(hex: 0xFFCA3A)).foregroundColor(Color(hex: 0x053324)).cornerRadius(15)

            Button("Niveaux") { vm.push(.levels(vm.selectedDifficulty)) }
              .frame(minHeight: 50).padding(.horizontal, 16)
              .font(.subheadline.bold())
              .foregroundColor(Color(hex: 0xFFFBF0))
              .overlay(RoundedRectangle(cornerRadius: 15).stroke(Color(hex: 0x2B8C6B), lineWidth: 1.5))
          }
        }
        .padding(16).background(Color(hex: 0x0E4735).opacity(0.95)).cornerRadius(22)
        .overlay(RoundedRectangle(cornerRadius: 22).stroke(Color(hex: 0x2B8C6B), lineWidth: 1.5))

        VStack(alignment: .leading, spacing: 10) {
          HStack {
            VStack(alignment: .leading) {
              Text("∞ Mode Sans Fin").font(.title3.bold()).foregroundColor(Color(hex: 0xFFFBF0))
              Text("Plateaux générés aléatoirement").font(.caption).foregroundColor(Color(hex: 0xB9DFCE))
            }
            Spacer()
            if vm.bestEndlessStreak > 0 {
              Text("Série : \(vm.bestEndlessStreak)")
                .font(.caption.bold()).foregroundColor(Color(hex: 0x56E39F))
                .padding(.horizontal, 10).padding(.vertical, 6)
                .background(Color(hex: 0x56E39F).opacity(0.16)).cornerRadius(10)
            }
          }
          Button("∞ Jouer Sans Fin (\(vm.selectedDifficulty.labelFr))") {
            vm.startEndless(difficulty: vm.selectedDifficulty, pack: vm.selectedPack)
          }
          .frame(maxWidth: .infinity, minHeight: 50)
          .font(.headline.bold())
          .background(Color(hex: 0xD97736)).foregroundColor(Color(hex: 0xFFFBF0)).cornerRadius(15)
        }
        .padding(16).background(Color(hex: 0x0E4735).opacity(0.95)).cornerRadius(22)
        .overlay(RoundedRectangle(cornerRadius: 22).stroke(Color(hex: 0x2B8C6B), lineWidth: 1.5))
      }
      .padding(18)
    }
  }
}

struct LevelMapView: View {
  @ObservedObject var vm: GameViewModel
  @State var currentDiff: Difficulty
  @State private var loadedCount = 36

  init(vm: GameViewModel, initialDifficulty: Difficulty) {
    self.vm = vm
    _currentDiff = State(initialValue: initialDifficulty)
  }

  var body: some View {
    VStack(spacing: 12) {
      HStack {
        WoodCircleBtn(icon: "arrow.left") { vm.goBack() }
        Spacer()
        VStack {
          Text("Niveaux Infinis").font(.headline.bold()).foregroundColor(Color(hex: 0xFFFBF0))
          Text("\(vm.selectedPack.titleFr) • ∞ Niveaux").font(.caption).foregroundColor(Color(hex: 0x56E39F))
        }
        Spacer()
        WoodCircleBtn(icon: "gearshape.fill") { vm.push(.settings) }
      }
      HStack(spacing: 8) {
        ForEach(Difficulty.allCases) { diff in
          let sel = currentDiff == diff
          Button {
            currentDiff = diff
            loadedCount = max(36, vm.unlockedLevel(for: diff) + 18)
          } label: {
            Text("\(diff.labelFr) • \(diff.labelAr)")
              .font(.caption.bold())
              .frame(maxWidth: .infinity).padding(.vertical, 10)
              .background(sel ? Color(hex: 0xD97736) : Color(hex: 0x0E4735))
              .foregroundColor(Color(hex: 0xFFFBF0)).cornerRadius(12)
          }
        }
      }
      ScrollView {
        let unlocked = vm.unlockedLevel(for: currentDiff)
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 80), spacing: 10)], spacing: 10) {
          ForEach(1...loadedCount, id: \.self) { lvl in
            let isCur = lvl == unlocked
            let isDone = lvl < unlocked
            Button {
              if lvl <= unlocked {
                vm.startCampaign(difficulty: currentDiff, level: lvl, pack: vm.selectedPack)
              }
            } label: {
              VStack(spacing: 4) {
                Text("\(lvl)").font(.title3.bold())
                Text(isDone ? "★★★" : (isCur ? "▶" : "🔒")).font(.caption2)
              }
              .frame(maxWidth: .infinity, minHeight: 74)
              .background(isCur ? Color(hex: 0xFFCA3A) : (isDone ? Color(hex: 0xFFFDF7) : Color(hex: 0x0E4735)))
              .foregroundColor((isCur || isDone) ? Color(hex: 0x053324) : Color(hex: 0xB9DFCE).opacity(0.6))
              .cornerRadius(16)
            }
            .disabled(lvl > unlocked)
            .onAppear {
              if lvl >= loadedCount - 4 { loadedCount += 18 }
            }
          }
        }
      }
    }
    .padding(16)
  }
}

struct GameBoardView: View {
  @ObservedObject var vm: GameViewModel

  var body: some View {
    ZStack {
      VStack(spacing: 8) {
        HStack {
          WoodCircleBtn(icon: "arrow.left") { vm.goBack() }
          Spacer()
          HStack(spacing: 24) {
            VStack {
              Text("Score").font(.caption.bold()).foregroundColor(Color(hex: 0x4BE39E))
              Text("\(vm.score)").font(.title2.bold()).foregroundColor(Color(hex: 0xFFFBF0))
            }
            VStack {
              Text("Paires").font(.caption.bold()).foregroundColor(Color(hex: 0x4BE39E))
              Text("\(vm.availablePairs)").font(.title2.bold()).foregroundColor(Color(hex: 0xFFFBF0))
            }
          }
          Spacer()
          WoodCircleBtn(icon: "gearshape.fill") { vm.push(.settings) }
        }

        HStack(spacing: 8) {
          Text(vm.isEndlessMode ? "∞ Sans fin #\(vm.currentLevel)" : "\(vm.selectedDifficulty.labelFr) • Niveau \(vm.currentLevel)")
            .font(.caption.bold()).foregroundColor(Color(hex: 0xFFFBF0))
            .padding(.horizontal, 12).padding(.vertical, 6)
            .background(Color(hex: 0x0E4735)).clipShape(Capsule())

          if let sym = vm.lastSpokenSymbol {
            Button {
              vm.audio.speakSymbol(sym)
            } label: {
              let label = sym.category == .arabicLetter
                ? "🔊 \(sym.glyph) • \(sym.arabicDisplayName ?? sym.glyph) / \(sym.frenchDisplayName)"
                : "🔊 \(sym.glyph) • \(sym.frenchDisplayName)"
              Text(label)
                .font(.caption.bold()).foregroundColor(Color(hex: 0x053324))
                .padding(.horizontal, 12).padding(.vertical, 6)
                .background(Color(hex: 0xFFCA3A)).clipShape(Capsule())
            }
          }
        }

        GeometryReader { geo in
          let tiles = vm.tiles
          if !tiles.isEmpty {
            let minX = tiles.map(\.x).min() ?? 0
            let maxX = tiles.map(\.x).max() ?? 0
            let minY = tiles.map(\.y).min() ?? 0
            let maxY = tiles.map(\.y).max() ?? 0
            let maxZ = tiles.map(\.z).max() ?? 0
            let cols = CGFloat(maxX - minX) / 2.0 + 1.0
            let rows = CGFloat(maxY - minY) / 2.0 + 1.0
            let tileW = min(max(36, min((geo.size.width - 20) / (cols + 0.35), (geo.size.height - 20) / (rows * 1.24 + 0.35))), 82)
            let tileH = tileW * 1.24
            let boardW = cols * tileW + CGFloat(maxZ) * 4 + 8
            let boardH = rows * tileH + CGFloat(maxZ) * 5 + 10
            let sorted = tiles.sorted { ($0.z, $0.y, $0.x) < ($1.z, $1.y, $1.x) }

            ZStack(alignment: .topLeading) {
              ForEach(sorted) { tile in
                let ox = CGFloat(tile.x - minX) / 2.0 * tileW + CGFloat(maxZ - tile.z) * 4
                let oy = CGFloat(tile.y - minY) / 2.0 * tileH + CGFloat(maxZ - tile.z) * 5
                TilePieceView(
                  tile: tile,
                  w: tileW,
                  h: tileH,
                  isSelected: vm.selectedTileId == tile.id,
                  isHinted: vm.hintTileIds.contains(tile.id),
                  dimBlocked: vm.highlightFreeTiles
                ) {
                  vm.tapTile(tile)
                }
                .offset(x: ox, y: oy)
              }
            }
            .frame(width: boardW, height: boardH)
            .position(x: geo.size.width / 2, y: geo.size.height / 2)
          }
        }

        HStack {
          Spacer()
          WoodDiamondBtn(icon: "arrow.triangle.2.circlepath", title: "Mélanger") { vm.shuffleBoard() }
          Spacer()
          Text("\(vm.tiles.count) tuiles")
            .font(.caption.bold()).foregroundColor(Color(hex: 0xB9DFCE))
            .padding(.horizontal, 14).padding(.vertical, 8)
            .background(Color(hex: 0x0E4735)).cornerRadius(14)
          Spacer()
          WoodDiamondBtn(icon: "lightbulb.fill", title: "Indice") { vm.showHint() }
          Spacer()
        }
      }
      .padding(14)

      if vm.isBoardComplete {
        Color.black.opacity(0.58).ignoresSafeArea()
        VStack(spacing: 14) {
          Text("★★★").font(.title).foregroundColor(Color(hex: 0xFFCA3A))
          Text("Bravo ! • أحسنت !").font(.title.bold()).foregroundColor(Color(hex: 0xFFCA3A))
          Text("Score : \(vm.score)").font(.headline).foregroundColor(Color(hex: 0x56E39F))
          Button("▶ Niveau suivant") { vm.nextBoard() }
            .frame(maxWidth: .infinity, minHeight: 50)
            .font(.headline.bold())
            .background(Color(hex: 0xFFCA3A)).foregroundColor(Color(hex: 0x053324)).cornerRadius(15)
          Button("Accueil") { vm.goHome() }
            .frame(maxWidth: .infinity, minHeight: 46)
            .foregroundColor(Color(hex: 0xFFFBF0))
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: 0x2B8C6B), lineWidth: 1.5))
        }
        .padding(24).background(Color(hex: 0x0E4735)).cornerRadius(26)
        .overlay(RoundedRectangle(cornerRadius: 26).stroke(Color(hex: 0xFFCA3A), lineWidth: 2))
        .padding(28)
      }
    }
  }
}

struct SettingsView: View {
  @ObservedObject var vm: GameViewModel

  var body: some View {
    ScrollView {
      VStack(spacing: 14) {
        HStack {
          WoodCircleBtn(icon: "arrow.left") { vm.goBack() }
          Spacer()
          Text("Paramètres • الإعدادات").font(.headline.bold()).foregroundColor(Color(hex: 0xFFFBF0))
          Spacer()
          Color.clear.frame(width: 48, height: 48)
        }
        VStack(spacing: 12) {
          Toggle("Musique calme", isOn: $vm.musicEnabled)
          Toggle("Effets sonores", isOn: $vm.sfxEnabled)
          Toggle("Voix (Prononciation)", isOn: $vm.voiceEnabled)
          Toggle("Prononcer dès le 1er appui", isOn: $vm.speakOnTap)
          Toggle("Assombrir tuiles bloquées", isOn: $vm.highlightFreeTiles)
          HStack {
            Button("🔊 Tester (1 • Un)") { vm.testFrench() }
              .frame(maxWidth: .infinity, minHeight: 44)
              .font(.subheadline.bold())
              .background(Color(hex: 0xFFCA3A)).foregroundColor(Color(hex: 0x053324)).cornerRadius(12)
            Button("🔊 Tester (أ • ألف)") { vm.testArabic() }
              .frame(maxWidth: .infinity, minHeight: 44)
              .font(.subheadline.bold())
              .background(Color(hex: 0x56E39F)).foregroundColor(Color(hex: 0x053324)).cornerRadius(12)
          }
        }
        .padding(16).background(Color(hex: 0x0E4735)).cornerRadius(20)
        .foregroundColor(Color(hex: 0xFFFBF0))

        VStack(alignment: .leading, spacing: 10) {
          Text("Langue des lettres arabes").font(.subheadline.bold()).foregroundColor(Color(hex: 0x56E39F))
          ForEach(ArabicVoiceMode.allCases) { mode in
            Button {
              vm.arabicVoiceMode = mode
            } label: {
              HStack {
                Image(systemName: vm.arabicVoiceMode == mode ? "largecircle.fill.circle" : "circle")
                  .foregroundColor(Color(hex: 0xFFCA3A))
                Text(mode.label).foregroundColor(Color(hex: 0xFFFBF0))
                Spacer()
              }.padding(.vertical, 6)
            }
          }
        }
        .padding(16).background(Color(hex: 0x0E4735)).cornerRadius(20)
      }
      .padding(16)
    }
  }
}

struct TilePieceView: View {
  let tile: BoardTile
  let w: CGFloat
  let h: CGFloat
  let isSelected: Bool
  let isHinted: Bool
  let dimBlocked: Bool
  let action: () -> Void

  var body: some View {
    let fg: Color = tile.symbol.category == .latinLetter
      ? Color(hex: 0xD63638)
      : (tile.symbol.category == .number ? Color(hex: 0x1958C9) : Color(hex: 0xB55309))

    ZStack {
      RoundedRectangle(cornerRadius: 10)
        .fill(LinearGradient(colors: [Color(hex: 0x43A038), Color(hex: 0x286E22)], startPoint: .top, endPoint: .bottom))
        .offset(x: 3.5, y: 4.5)
        .shadow(color: .black.opacity(0.35), radius: 3, x: 1, y: 3)

      RoundedRectangle(cornerRadius: 10)
        .fill(LinearGradient(colors: [Color(hex: 0xFFFDF7), Color(hex: 0xF3EAD8)], startPoint: .top, endPoint: .bottom))
        .overlay(
          RoundedRectangle(cornerRadius: 10)
            .stroke(isSelected ? Color(hex: 0xFFB300) : (isHinted ? Color(hex: 0x29D3FF) : Color(hex: 0xE3D8C5)), lineWidth: (isSelected || isHinted) ? 2.5 : 1)
        )

      Text(tile.symbol.glyph)
        .font(.system(size: w * (tile.symbol.glyph.count > 1 ? 0.45 : 0.56), weight: .heavy, design: .rounded))
        .foregroundColor(fg)

      if !tile.isFree && dimBlocked {
        RoundedRectangle(cornerRadius: 10).fill(Color(hex: 0x12382A).opacity(0.36))
      }
    }
    .frame(width: w, height: h)
    .scaleEffect(isSelected ? 1.06 : 1.0)
    .offset(y: isSelected ? -4 : 0)
    .onTapGesture(perform: action)
  }
}

struct MiniTile: View {
  let glyph: String
  let color: Color
  let angle: Double

  var body: some View {
    Text(glyph)
      .font(.system(size: 26, weight: .black, design: .rounded))
      .foregroundColor(color)
      .frame(width: 46, height: 56)
      .background(Color(hex: 0xFFFDF7))
      .cornerRadius(10)
      .shadow(color: Color(hex: 0x286E22), radius: 0, x: 3, y: 4)
      .rotationEffect(.degrees(angle))
  }
}

struct WoodCircleBtn: View {
  let icon: String
  let action: () -> Void

  var body: some View {
    Button(action: action) {
      Image(systemName: icon)
        .font(.system(size: 18, weight: .bold))
        .foregroundColor(Color(hex: 0xFFCA3A))
        .frame(width: 48, height: 48)
        .background(LinearGradient(colors: [Color(hex: 0xD97736), Color(hex: 0x8F3E14)], startPoint: .top, endPoint: .bottom))
        .clipShape(Circle())
        .overlay(Circle().stroke(Color(hex: 0xF5B763), lineWidth: 2))
    }
  }
}

struct WoodDiamondBtn: View {
  let icon: String
  let title: String
  let action: () -> Void

  var body: some View {
    Button(action: action) {
      VStack(spacing: 8) {
        ZStack {
          RoundedRectangle(cornerRadius: 12)
            .fill(LinearGradient(colors: [Color(hex: 0xD97736), Color(hex: 0x8F3E14)], startPoint: .top, endPoint: .bottom))
            .frame(width: 46, height: 46)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: 0xF5B763), lineWidth: 2))
            .rotationEffect(.degrees(45))
          Image(systemName: icon)
            .font(.system(size: 18, weight: .bold))
            .foregroundColor(Color(hex: 0xFFCA3A))
        }
        Text(title).font(.caption2.bold()).foregroundColor(Color(hex: 0xFFFBF0))
      }
    }
  }
}

extension Color {
  init(hex: UInt, alpha: Double = 1.0) {
    self.init(
      .sRGB,
      red: Double((hex >> 16) & 0xFF) / 255.0,
      green: Double((hex >> 8) & 0xFF) / 255.0,
      blue: Double(hex & 0xFF) / 255.0,
      opacity: alpha
    )
  }
}
