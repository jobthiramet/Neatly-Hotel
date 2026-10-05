// Figma: Chatbot/Virtual Concierge (13259:21785) + Admin Chatbot Setup copy

export const chatbotGreeting
  = 'Welcome to Neatly Hotel! 🌟 I\'m your virtual assistant. Choose a topic you\'d like to know more about. I\'m here to help! 😊'

export const chatbotAutoReply
  = 'Thanks for reaching out to us! If you need any more help, just give us a call at 020872345 we\'re happy to assist you! 🧡'

/** Room slugs shown on Figma Chatbot Setup room-type cards. */
export const chatbotDefaultRoomIds = [
  'superior-garden-view',
  'deluxe',
  'superior',
  'supreme',
] as const

export type ChatbotPaymentOption = {
  label: string
  detail: string
}

export type ChatbotCta = {
  text: string
  actionLabel: string
  to: string
  query?: Record<string, string>
}

export const chatbotGuestBookingCta: ChatbotCta = {
  text: 'Please log in to start a booking. 🧡',
  actionLabel: 'Log in',
  to: '/sign-in',
}

export const chatbotGuestCancelCta: ChatbotCta = {
  text: 'Please log in to cancel a booking. 🧡',
  actionLabel: 'Log in',
  to: '/sign-in',
  query: { redirect_url: '/booking-history' },
}

export const chatbotSignedInCancelCta: ChatbotCta = {
  text: 'You can cancel a booking from your booking history. 🧡',
  actionLabel: 'Booking History',
  to: '/booking-history',
}

export const chatbotCancelTopic = {
  id: 'cancel-booking',
  label: 'Cancel Booking',
  enabled: true,
} as const

export type ChatbotReplyFormat = 'message' | 'room-type' | 'option-with-details'

export type ChatbotTopic
  = {
    id: string
    label: string
    enabled: true
    format: 'message'
    text: string
  }
  | {
    id: string
    label: string
    enabled: true
    format: 'room-type'
    title: string
    actionLabel: string
    roomIds: string[]
  }
  | {
    id: string
    label: string
    enabled: true
    format: 'option-with-details'
    title: string
    options: ChatbotPaymentOption[]
  }

export const chatbotTopics: ChatbotTopic[] = [
  {
    id: 'room-types',
    label: 'Room Types',
    enabled: true,
    format: 'room-type',
    title: 'Neatly Hotel offers a variety of room types to suit your needs! 🏨✨ Here are the options.',
    actionLabel: 'View Details',
    roomIds: [...chatbotDefaultRoomIds],
  },
  {
    id: 'booking',
    label: 'Booking',
    enabled: true,
    format: 'room-type',
    title: 'Let\'s get your booking started. First, please choose the type of room you\'d like 🏨✨.',
    actionLabel: 'Book Now',
    roomIds: [...chatbotDefaultRoomIds],
  },
  {
    id: 'check-in-out',
    label: 'Check-in & Check-out Time',
    enabled: true,
    format: 'message',
    text: 'Great! 😊 Here are our check-in and check-out times:\n\nCheck-in time: From 2:00 PM onwards 🕒\nCheck-out time: By 12:00 PM 🕛',
  },
  {
    id: 'payment-methods',
    label: 'Payment Methods',
    enabled: true,
    format: 'option-with-details',
    title: 'Here are the payment methods we accept. Tap to see more details 💳💰.',
    options: [
      {
        label: 'Credit Card',
        detail: 'We accept credit cards including Visa and MasterCard.',
      },
      {
        label: 'Cash',
        detail: 'You can pay at the hotel with cash or cheque. No payment is required until check-in.',
      },
      {
        label: 'QR Code',
        detail: 'You can pay by scanning a QR code, including PromptPay.',
      },
    ],
  },
  {
    id: 'promotion',
    label: 'Promotion',
    enabled: true,
    format: 'room-type',
    title: '🎉 Our promotion this month: Get 10% off 💰 when you book your stay within this month. Don\'t miss out!',
    actionLabel: 'Book Now',
    roomIds: [...chatbotDefaultRoomIds],
  },
]

export function findChatbotTopic(topics: ChatbotTopic[], text: string): ChatbotTopic | undefined {
  const needle = text.trim().toLowerCase()
  if (!needle) return undefined
  return topics.find(topic => topic.label.toLowerCase() === needle)
}

export function isChatbotCancelLabel(text: string) {
  return text.trim().toLowerCase() === chatbotCancelTopic.label.toLowerCase()
}
